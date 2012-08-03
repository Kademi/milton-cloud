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
import io.milton.cloud.server.db.MediaMetaData;
import io.milton.cloud.server.web.FileResource;
import io.milton.common.ContentTypeService;
import io.milton.common.FileUtils;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.event.PutEvent;
import io.milton.vfs.db.utils.SessionManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    private final RootContext rootContext;
    private final SessionManager sessionManager;
    private final ContentTypeService contentTypeService;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final MediaInfoService mediaInfoService;
    private String ffmpeg = "avconv";
    private List<FormatSpec> formats;
    private ExecutorService consumer = new ThreadPoolExecutor(1, 4, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));
    private final List<GenerateJob> currentJobs = new CopyOnWriteArrayList<>();

    private boolean enableMetaData;
    
    public AltFormatGenerator(HashStore hashStore, BlobStore blobStore, EventManager eventManager, ContentTypeService contentTypeService, RootContext rootContext, SessionManager sessionManager) {
        this.rootContext = rootContext;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.contentTypeService = contentTypeService;
        this.sessionManager = sessionManager;
        this.formats = new ArrayList<>();
        this.mediaInfoService = new MediaInfoService(hashStore, blobStore);
        formats.add(new FormatSpec("image", "png", 150, 150, "-ss", "1", "-vframes", "1", "-f", "mjpeg"));

        formats.add(new FormatSpec("video", "flv", 800, 455, "-r", "15", "-b:v", "512k")); // for non-html video
        formats.add(new FormatSpec("video", "mp4", 800, 455, "-c:v", "mpeg4", "-r", "15", "-b:v", "512k")); // for ipad
        formats.add(new FormatSpec("video", "ogv", 800, 455, "-r", "15", "-b:v", "512k"));
        //formats.add(new FormatSpec("video", "webm", 800, 455));

        formats.add(new FormatSpec("video", "png", 800, 455, "-ss", "1", "-vframes", "1", "-f", "mjpeg"));

        System.out.println("register put event on: " + eventManager);
        eventManager.registerEventListener(this, PutEvent.class);
    }

    @Override
    public void onEvent(Event e) {
        System.out.println("onEvent: " + e);
        if( !enableMetaData ) {
            System.out.println("meta data generation is not enabled");
            return ;
        }
        if (e instanceof PutEvent) {
            System.out.println("");
            PutEvent pe = (PutEvent) e;
            if (pe.getResource() instanceof FileResource) {
                FileResource fr = (FileResource) pe.getResource();
                if (isMedia(fr)) {
                    System.out.println("is media, generate metadata");
                    findInfo(fr);
                }
            }
        }
    }

    private void findInfo(FileResource fr) {
        MediaMetaData mmd = MediaMetaData.find(fr.getHash(), SessionManager.session());
        if (mmd != null) {
            System.out.println("already have meta data record");
            return;
        }

        try {
            System.out.println("get nifo");
            MediaInfo info = mediaInfoService.getInfo(fr);
            if (info == null) {
                log.warn("Null info for: " + fr.getHref());
                return;
            }
            System.out.println("create new media metadata");
            mmd = new MediaMetaData();
            mmd.setSourceHash(fr.getHash());
            mmd.setDurationSecs(info.getDurationSecs());
            mmd.setHeight(info.getHeight());
            mmd.setWidth(info.getWidth());
            mmd.setRecordedDate(info.getRecordedDate());
            SessionManager.session().save(mmd);
            
        } catch (IOException ex) {
            log.error("Couldnt get media info for: " + fr.getHref(), ex);
        }
    }

    /**
     * Returns an existing job if there is one that matches the inputs,
     * otherwise attempts to create a new job and enqueue it. If it cannot
     * insert because of capacity constraints it will fail
     *
     * @param primaryHash
     * @param fileName
     * @param format
     * @return
     */
    public GenerateJob getOrEnqueueJob(String primaryHash, String fileName, FormatSpec format) {
        for (GenerateJob j : currentJobs) {
            if (j.primaryFileHash == primaryHash) {
                if (format.equals(j.formatSpec)) {
                    return j;
                }
            }
        }
        GenerateJob j = new GenerateJob(primaryHash, fileName, format);
        currentJobs.add(j);
        System.out.println("submitted new job");
        consumer.submit(j);
        return j;
    }

    public FormatSpec findFormat(String name) {
        for (FormatSpec f : formats) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
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

    public class GenerateJob implements Runnable {

        private static final long serialVersionUID = 1l;
        private final String primaryFileHash;
        private final FormatSpec formatSpec;
        private final String primaryFileName;
        private final AvconvConverter converter;
        private boolean jobDone;

        public GenerateJob(String primaryFileHash, String primaryFileName, FormatSpec formatSpec) {
            this.primaryFileHash = primaryFileHash;
            this.primaryFileName = primaryFileName;
            this.formatSpec = formatSpec;
            if (formatSpec == null) {
                throw new RuntimeException("formatSpec cannot be null");
            }
            String ext = FileUtils.getExtension(primaryFileName);
            converter = new AvconvConverter(ffmpeg, primaryFileHash, primaryFileName, formatSpec, ext, contentTypeService, hashStore, blobStore);
        }

        public File getDestFile() {
            return converter.getDest();
        }

        @Override
        public void run() {
            System.out.println("GenerateJob: run");
            try {
                rootContext.execute(new Executable2() {

                    @Override
                    public void execute(Context context) {
                        sessionManager.open();
                        try {
                            doProcess(context);
                        } finally {
                            sessionManager.close();
                        }
                    }
                });
            } catch (Throwable e) {
                log.error("Exception in generate job", e);
            } finally {
                jobDone = true;
                currentJobs.remove(this);
            }
            System.out.println("Generate job finished");
        }

        public void doProcess(Context context) {
            Transaction tx = SessionManager.session().beginTransaction();
            try {
                generate();
                tx.commit();
            } catch (IOException ex) {
                tx.rollback();
                Logger.getLogger(AltFormatGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public AltFormat generate() throws IOException {
            final Parser parser = new Parser();
            String altHash;
            try {
                altHash = converter.generate(new With<InputStream, String>() {

                    @Override
                    public String use(InputStream t) throws Exception {
                        String newFileHash = parser.parse(t, hashStore, blobStore);
                        return newFileHash;
                    }
                });
            } catch (Exception e) {
                throw new IOException("Couldnt convert: " + primaryFileName, e);
            }
            if (altHash != null) {
                String name = formatSpec.getName();
                return AltFormat.insertIfOrUpdate(name, primaryFileHash, altHash, SessionManager.session());
            } else { 
                return null;
            }
        }

        /**
         * True when the job is completed
         *
         * @return
         */
        public boolean done() {
            return jobDone;
        }
    }

    public boolean isEnableMetaData() {
        return enableMetaData;
    }

    public void setEnableMetaData(boolean enableMetaData) {
        this.enableMetaData = enableMetaData;
    }
    
    
}
