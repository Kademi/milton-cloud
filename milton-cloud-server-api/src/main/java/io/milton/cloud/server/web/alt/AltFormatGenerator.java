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
import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.queue.Processable;
import io.milton.cloud.server.web.FileResource;
import io.milton.common.ContentTypeService;
import io.milton.common.FileUtils;
import io.milton.context.Context;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.event.PutEvent;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;
import org.hibernate.Transaction;

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
    private final AsynchProcessor asynchProcessor;
    private String ffmpeg = "avconv";
    private List<FormatSpec> formats;

    public AltFormatGenerator(HashStore hashStore, BlobStore blobStore, EventManager eventManager, ContentTypeService contentTypeService, AsynchProcessor asynchProcessor) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.contentTypeService = contentTypeService;
        this.formats = new ArrayList<>();
        this.asynchProcessor = asynchProcessor;
        formats.add(new FormatSpec("image", "png", 150, 150));

        formats.add(new FormatSpec("video", "flv", 800, 455)); // for non-html video
        formats.add(new FormatSpec("video", "mp4", 800, 455)); // for ipad
        formats.add(new FormatSpec("video", "ogv", 800, 455));
        //formats.add(new FormatSpec("video", "webm", 800, 455));

        formats.add(new FormatSpec("video", "png", 800, 455));
        eventManager.registerEventListener(this, PutEvent.class);
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof PutEvent) {
            PutEvent pe = (PutEvent) e;
            if (pe.getResource() instanceof FileResource) {
                FileResource fr = (FileResource) pe.getResource();
                if (isMedia(fr)) {
                    Long l = fr.getContentLength();
                    if( l != null && l > 20000) { // Must be at least 20k, otherwise don't bother
                        onPut(fr);
                    }
                }
            }
        }
    }

    private void onPut(FileResource fr) {
        log.info("onPut: enqueueing: " + fr.getName());
        GenerateJob job = new GenerateJob(fr.getHash(), fr.getName());
        asynchProcessor.enqueue(job);
    }

    private void generate(long primaryMediaHash, String name) throws IOException {
        String ext = FileUtils.getExtension(name);

        AvconvConverter converter = new AvconvConverter(ffmpeg, primaryMediaHash, name, ext, contentTypeService, hashStore, blobStore);
        if (formats != null) {
            for (FormatSpec f : formats) {
                if (is(name, f.inputType)) {
                    generate(f, primaryMediaHash, converter, name);
                }
            }
        }
    }

    public AltFormat generate(FormatSpec f, FileResource fr) throws IOException {
        String ext = FileUtils.getExtension(fr.getName());
        AvconvConverter converter = new AvconvConverter(ffmpeg, fr.getHash(), fr.getName(), ext, contentTypeService, hashStore, blobStore);
        return generate(f, fr.getHash(), converter, fr.getHref());
    }

    public AltFormat generate(FormatSpec f, long primaryHash, AvconvConverter converter, String primaryName) throws IOException {
        final Parser parser = new Parser();
        Long altHash;
        try {
            altHash = converter.generate(f, new With<InputStream, Long>() {

                @Override
                public Long use(InputStream t) throws Exception {
                    long numBytes = parser.parse(t, hashStore, blobStore);
                    return numBytes;
                }
            });
        } catch (Exception e) {
            throw new IOException("Couldnt convert: " + primaryName, e);
        }
        if (altHash != null) {
            String name = f.getName();
            return AltFormat.insertIfOrUpdate(name, primaryHash, altHash, SessionManager.session());
        } else {
            return null;
        }
    }

    public FormatSpec findFormat(String name) {
        for (FormatSpec f : formats) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    private boolean is(String name, String type) {
        // will return a non-null value if type is contained in any content type
        List<String> list = contentTypeService.findContentTypes(name);
        if (list != null) {
            for (String ct : list) {
                if (ct.contains(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMedia(FileResource fr) {
        if (formats != null) {
            String name = fr.getName();
            for (FormatSpec f : formats) {
                if (is(name, f.inputType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public class GenerateJob implements Processable {

        private static final long serialVersionUID = 1l;
        private long primaryFileHash;
        private String primaryFileName;

        public GenerateJob(long primaryFileHash, String primaryFileName) {
            this.primaryFileHash = primaryFileHash;
            this.primaryFileName = primaryFileName;
        }

        @Override
        public void doProcess(Context context) {
            Transaction tx = SessionManager.session().beginTransaction();
            try {
                // TODO: this relies on keeping a reference to the AltFormatGenerator. But need to decouple to support
                // distributed queues
                generate(primaryFileHash, primaryFileName);
                tx.commit();
            } catch (IOException ex) {
                tx.rollback();
                Logger.getLogger(AltFormatGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void pleaseImplementSerializable() {
        }
    }
}
