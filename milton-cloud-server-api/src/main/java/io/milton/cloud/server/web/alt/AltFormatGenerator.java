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
        formats.add(new FormatSpec("png", 150, 150));
        formats.add(new FormatSpec("flv", 800, 600));
        formats.add(new FormatSpec("png", 800, 600));
        eventManager.registerEventListener(this, PutEvent.class);
    }
    
    public FormatSpec findFormat(String name) {
        System.out.println("findFormat: " + name);
        for( FormatSpec f : formats) {
            System.out.println("? " + name + " = " + f.getName());
            if( f.getName().equals(name)) {
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

    public AltFormat generate(FormatSpec f, FileResource fr) throws IOException {
        String ext = FileUtils.getExtension(fr.getName());
        FFMPEGConverter converter = new FFMPEGConverter(ffmpeg, fr, ext);        
        return generate(f, fr, converter);
    }
    
    public AltFormat generate(FormatSpec f, FileResource fr, FFMPEGConverter converter) throws IOException {
        final Parser parser = new Parser();
        long altHash = converter.generate(f, new With<InputStream, Long>() {

            @Override
            public Long use(InputStream t) throws Exception {
                return parser.parse(t, hashStore, blobStore);                
            }
        });        
        String name = f.getName();
        return AltFormat.insertIfOrUpdate(name, fr.getHash(), altHash, SessionManager.session());
    }
    

    private void onPut(FileResource fr) {
        log.info("onPut: " + fr.getName());
        if (isVideo(fr)) {
            try {
                generate(fr);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            log.info("Not supported type");
        }
    }

    private boolean isVideo(FileResource fr) {
        List<String> list = contentTypeService.findContentTypes(fr.getName());
        for( String s : list ) {
            if( s.contains("video")) {
                return true;
            }
        }
        return false;
    }

    private void generate(FileResource fr) throws IOException {
        String ext = FileUtils.getExtension(fr.getName());
        FFMPEGConverter converter = new FFMPEGConverter(ffmpeg, fr, ext);
        if (formats != null) {
            for (FormatSpec f : formats) {
                generate(f, fr, converter);
            }
        }
//        if (videoFormats != null) {
//            for (Format f : videoFormats) {
//                bout.reset();
//                log.info("generate thumb: " + f.getName());
//                converter.convert(bout, f.type, f.height, f.width);
//                Parser parser = new Parser();
//                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
//                long altHash = parser.parse(bin, hashStore, blobStore);
//                String name = f.getName();
//                AltFormat.insertIfOrUpdate(name, fr.getHash(), altHash, SessionManager.session());
//            }
//        }


    }
}
