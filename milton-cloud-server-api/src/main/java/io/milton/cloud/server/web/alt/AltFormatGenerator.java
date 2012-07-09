/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.alt;

import io.milton.cloud.common.With;
import io.milton.cloud.server.db.AltFormat;
import io.milton.cloud.server.web.FileResource;
import io.milton.common.ContentTypeService;
import io.milton.common.FileUtils;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.event.PutEvent;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;

/**
 * Listens for PUT events and generates alternative file formats as appropriate
 *
 * @author brad
 */
public class AltFormatGenerator implements EventListener {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AltFormatGenerator.class);
    private final ContentTypeService contentTypeService;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private String ffmpeg = "avconv";
    private List<FormatSpec> formats;

    public AltFormatGenerator(HashStore hashStore, BlobStore blobStore, EventManager eventManager, ContentTypeService contentTypeService) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.contentTypeService = contentTypeService;
        this.formats = new ArrayList<>();
        formats.add(new FormatSpec("image", "png", 150, 150));

        formats.add(new FormatSpec("video", "flv", 800, 455));
        formats.add(new FormatSpec("video", "mp4", 800, 455));
        formats.add(new FormatSpec("video", "ogv", 800, 455));
        formats.add(new FormatSpec("video", "webm", 800, 455));

        formats.add(new FormatSpec("video", "png", 800, 455));
        eventManager.registerEventListener(this, PutEvent.class);
    }

    public FormatSpec findFormat(String name) {
        System.out.println("findFormat: " + name);
        for (FormatSpec f : formats) {
            System.out.println("? " + name + " = " + f.getName());
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof PutEvent) {
            PutEvent pe = (PutEvent) e;
            if (pe.getResource() instanceof FileResource) {
                FileResource fr = (FileResource) pe.getResource();
                onPut(fr);
            }
        }
    }

    private void onPut(FileResource fr) {
        log.info("onPut: " + fr.getName());
        try {
            generate(fr);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void generate(FileResource file) throws IOException {
        String ext = FileUtils.getExtension(file.getName());
        AvconvConverter converter = new AvconvConverter(ffmpeg, file, ext, contentTypeService);
        if (formats != null) {
            for (FormatSpec f : formats) {
                if (file.is(f.inputType)) {
                    generate(f, file, converter);
                }
            }
        }
    }

    public AltFormat generate(FormatSpec f, FileResource fr) throws IOException {
        String ext = FileUtils.getExtension(fr.getName());
        AvconvConverter converter = new AvconvConverter(ffmpeg, fr, ext, contentTypeService);
        return generate(f, fr, converter);
    }

    public AltFormat generate(FormatSpec f, FileResource fr, AvconvConverter converter) throws IOException {
        final Parser parser = new Parser();
        Long altHash = converter.generate(f, new With<InputStream, Long>() {

            @Override
            public Long use(InputStream t) throws Exception {
                return parser.parse(t, hashStore, blobStore);
            }
        });
        if (altHash != null) {
            String name = f.getName();
            return AltFormat.insertIfOrUpdate(name, fr.getHash(), altHash, SessionManager.session());
        } else {
            return null;
        }
    }
}
