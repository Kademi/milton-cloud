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
import io.milton.cloud.server.db.HlsPrimary;
import io.milton.cloud.server.db.HlsProgram;
import io.milton.cloud.server.db.HlsSegment;
import io.milton.cloud.server.db.MediaMetaData;
import io.milton.cloud.server.manager.CurrentRootFolderService;
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
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Listens for PUT events and generates alternative file formats as appropriate
 *
 * Ubuntu commands to setup native dependencies: sudo apt-get install
 * libav-tools sudo apt-get install mediainfo sudo apt-get install
 * libavcodec-extra-53
 *
 * @author brad
 */
public class AltFormatGenerator implements EventListener {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AltFormatGenerator.class);
    
    public static FormatSpec HLS_FORMAT_SPEC = new FormatSpec("video", "m3u8");
    
    private final RootContext rootContext;
    private final SessionManager sessionManager;
    private final ContentTypeService contentTypeService;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final MediaInfoService mediaInfoService;
    private final CurrentRootFolderService currentRootFolderService;
    private String ffmpeg = "avconv";
    private List<FormatSpec> formats;
    private ExecutorService consumer = new ThreadPoolExecutor(10, 20, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));
    private final List<GenerateJob> currentJobs = new CopyOnWriteArrayList<>();
    private final FormatSpec profileSpec;
    private boolean enableMetaData;

    public AltFormatGenerator(HashStore hashStore, BlobStore blobStore, EventManager eventManager, ContentTypeService contentTypeService, RootContext rootContext, SessionManager sessionManager, CurrentRootFolderService currentRootFolderService) {
        this.rootContext = rootContext;
        this.currentRootFolderService = currentRootFolderService;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.contentTypeService = contentTypeService;
        this.sessionManager = sessionManager;
        this.formats = new ArrayList<>();
        this.mediaInfoService = new MediaInfoService(hashStore, blobStore);
        formats.add(new FormatSpec("image", "png", 150, 150, true, "-f", "image2"));
        formats.add(new FormatSpec("image", "png", 300, 300, true, "-f", "image2"));
        formats.add(new FormatSpec("image", "png", 600, 400, true, "-f", "image2"));
        profileSpec = new FormatSpec("image", "png", 52, 52, true, "-f", "image2");
        formats.add(profileSpec);

        formats.add(new FormatSpec("video", "flv", 1280, 720, false, "-r", "15", "-b:v", "512k")); // for non-html video
        formats.add(new FormatSpec("video", "m4v", 1280, 720, false, "-c:v", "libx264", "-r", "15", "-b:v", "512k")); // for ipad
        formats.add(new FormatSpec("video", "ogv", 1280, 720, false, "-r", "15", "-b:v", "512k"));
        formats.add(new FormatSpec("video", "webm", 1280, 720, false, "-r", "15", "-b:v", "512k"));

        formats.add(new FormatSpec("video", "flv", 640, 360, false, "-r", "15", "-b:v", "512k")); // for non-html video
        formats.add(new FormatSpec("video", "m4v", 640, 360, false, "-c:v", "libx264", "-r", "15", "-b:v", "512k")); // for ipad
        formats.add(new FormatSpec("video", "ogv", 640, 360, false, "-r", "15", "-b:v", "512k"));
        formats.add(new FormatSpec("video", "webm", 640, 360, false, "-r", "15", "-b:v", "512k"));
        // HTTP Live Streaming

        formats.add(HLS_FORMAT_SPEC);
        //formats.add(new FormatSpec("video", "m3u8", 640, 360, false, "-c:v", "libx264", "-c:a", "libvo_aacenc", "-map", "0", "-bsf", "h264_mp4toannexb", "-flags", "-global_header", "-f", "segment", "-segment_list_size", "99999", "-segment_format", "mpegts"));

        // Video thumbnails
        formats.add(new FormatSpec("video", "png", 640, 360, false, "-ss", "0", "-vframes", "1", "-f", "image2"));
        formats.add(new FormatSpec("video", "png", 1280, 720, false, "-ss", "0", "-vframes", "1", "-f", "image2"));

        System.out.println("register put event on: " + eventManager);
        eventManager.registerEventListener(this, PutEvent.class);
        rootContext.put(this);
    }

    @Override
    public void onEvent(Event e) {
        System.out.println("onEvent: " + e);
        if (!enableMetaData) {
            System.out.println("meta data generation is not enabled");
            return;
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
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            mmd = new MediaMetaData();
            mmd.setSourceHash(fr.getHash());
            mmd.setDurationSecs(info.getDurationSecs());
            mmd.setHeight(info.getHeight());
            mmd.setWidth(info.getWidth());
            mmd.setRecordedDate(info.getRecordedDate());
            SessionManager.session().save(mmd);
            tx.commit();
        } catch (IOException ex) {
            log.error("Couldnt get media info for: " + fr.getHref(), ex);
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

    /**
     * Generate a profile image (as defined in profileSpec) and return its hash
     *
     * @param primaryFileHash
     * @param primaryFileName
     * @return
     */
    public String generateProfileImage(String primaryFileHash, String primaryFileName) throws Exception {
        final Parser parser = new Parser();
        String ext = FileUtils.getExtension(primaryFileName);
        AvconvConverter converter = new AvconvConverter(ffmpeg, primaryFileHash, primaryFileName, profileSpec, ext, contentTypeService, hashStore, blobStore, mediaInfoService, currentRootFolderService, rootContext, sessionManager);
        String altHash = converter.generate(new With<InputStream, String>() {
            @Override
            public String use(InputStream t) throws Exception {
                String newFileHash = parser.parse(t, hashStore, blobStore);
                return newFileHash;
            }
        });
        return altHash;
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
            if (j.getPrimaryFileHash() == null ? primaryHash == null : j.getPrimaryFileHash().equals(primaryHash)) {
                if (format.equals(j.getFormatSpec())) {
                    log.info("Found existing job: " + j.getFormatSpec());
                    return j;
                }
            }
        }
        GenerateJob j;
        if( format.type.equals("m3u8")) {
            j = new HlsGenerateJob(primaryHash, primaryHash, format);
        } else {
            j = new VodGenerateJob(primaryHash, fileName, format);
        }
        currentJobs.add(j);
        log.info("submitted new job");
        consumer.submit(j);
        return j;
    }

    public interface GenerateJob extends Runnable {

        String getPrimaryFileHash();

        FormatSpec getFormatSpec();

        String getStatus();
        
        boolean done();
    }

    public class HlsGenerateJob implements GenerateJob, HlsGeneratorListener {

        private static final long serialVersionUID = 1l;
        private final String primaryFileHash;
        private final FormatSpec formatSpec;
        private final String primaryFileName;
        private final AvconvConverter converter;
        private boolean jobDone;
        private String status = "not started";
        private boolean done;
        

        public HlsGenerateJob(String primaryFileHash, String primaryFileName, FormatSpec formatSpec) {
            this.primaryFileHash = primaryFileHash;
            this.primaryFileName = primaryFileName;
            this.formatSpec = formatSpec;
            if (formatSpec == null) {
                throw new RuntimeException("formatSpec cannot be null");
            }
            String ext = FileUtils.getExtension(primaryFileName);
            converter = new AvconvConverter(ffmpeg, primaryFileHash, primaryFileName, formatSpec, ext, contentTypeService, hashStore, blobStore, mediaInfoService, currentRootFolderService, rootContext, sessionManager);
        }

        @Override
        public String getPrimaryFileHash() {
            return primaryFileHash;
        }

        @Override
        public FormatSpec getFormatSpec() {
            return formatSpec;
        }

        @Override
        public void run() {
            try {
                status = "generating";
                sessionManager.open();
                Transaction tx = SessionManager.beginTx();
                converter.generateHlsVideo(this, 3);
                tx.commit();
                status = "complete";
            } catch (Exception e) {
                status = "conversion failed";
                log.error("Exception", e);
            } finally {
                jobDone = true;
                currentJobs.remove(this);
                sessionManager.close();
            }
        }

        @Override
        public long onNewPrimary(int targetDuration, int version) {
            log.info("onNewPrimary: primaryFileHash=" + primaryFileHash);
            try {
                sessionManager.open();
                Transaction tx = SessionManager.beginTx();
                
                HlsPrimary p = new HlsPrimary();
                p.setSourceHash(primaryFileHash);
                p.setTargetDuration(targetDuration);
                p.setPrograms(new ArrayList());
                SessionManager.session().save(p);
                SessionManager.session().flush();
                long hlsPrimaryId = p.getId();
                
                tx.commit();
                return hlsPrimaryId;
            } catch (Throwable e) {
                log.error("ex", e);
                throw new RuntimeException(e);
            } finally {
                sessionManager.close();
            }
        }

        @Override
        public long onNewProgram(long primaryId, Dimension size, Integer likelyBandwidth) {
            log.info("onNewProgram: primary=" + primaryId);
            try {
                sessionManager.open();
                Transaction tx = SessionManager.beginTx();
                
                HlsPrimary p = HlsPrimary.get(primaryId, SessionManager.session());
                HlsProgram prog = p.addProgram(size.getWidth(), size.getHeight(), likelyBandwidth);
                SessionManager.session().save(prog);
                SessionManager.session().save(p);
                SessionManager.session().flush();
                
                tx.commit();
                return prog.getId();
            } catch (Throwable e) {
                log.error("ex", e);
                throw new RuntimeException(e);
            } finally {
                sessionManager.close();
            }
        }

        @Override
        public void onNewSegment(long progId, int seq, InputStream segmentData, Double duration) {
            log.info("onNewSegment: progId=" + progId);
            try {
                sessionManager.open();
                Transaction tx = SessionManager.beginTx();
                
                HlsProgram prog = HlsProgram.get(progId, SessionManager.session());
                if( prog == null ) {
                    throw new RuntimeException("Couldnt find program: " + progId);
                }
                Parser parser = new Parser();
                String segmentHash = parser.parse(segmentData, hashStore, blobStore);
                HlsSegment seg = prog.addSegment(segmentHash, seq);
                seg.setDurationSecs(duration);
                SessionManager.session().save(seg);
                SessionManager.session().flush();
                
                tx.commit();

            } catch (Throwable e) {
                log.error("ex", e);
            } finally {
                sessionManager.close();
            }
        }

        @Override
        public void onComplete(long primaryId) {
            try {
                sessionManager.open();
                Transaction tx = SessionManager.beginTx();
                
                HlsPrimary p = HlsPrimary.get(primaryId, SessionManager.session());
                p.setComplete(true);
                SessionManager.session().save(p);
                SessionManager.session().flush();
                
                tx.commit();
            } catch (Throwable e) {
                log.error("ex", e);
                throw new RuntimeException(e);
            } finally {
                sessionManager.close();
            }
        }

        @Override
        public String getStatus() {
            return status;
        }
        
        public boolean done() {
            return done;
        }
    }

    public class VodGenerateJob implements GenerateJob {

        private static final long serialVersionUID = 1l;
        private final String primaryFileHash;
        private final FormatSpec formatSpec;
        private final String primaryFileName;
        private final AvconvConverter converter;
        private boolean jobDone;
        private String status = "not started";

        public VodGenerateJob(String primaryFileHash, String primaryFileName, FormatSpec formatSpec) {
            this.primaryFileHash = primaryFileHash;
            this.primaryFileName = primaryFileName;
            this.formatSpec = formatSpec;
            if (formatSpec == null) {
                throw new RuntimeException("formatSpec cannot be null");
            }
            String ext = FileUtils.getExtension(primaryFileName);
            converter = new AvconvConverter(ffmpeg, primaryFileHash, primaryFileName, formatSpec, ext, contentTypeService, hashStore, blobStore, mediaInfoService, currentRootFolderService, rootContext, sessionManager);
        }

        public File getDestFile() {
            return converter.getGeneratedOutputFile();
        }

        public String getStatus() {
            return status;
        }

        @Override
        public void run() {
            System.out.println("GenerateJob: run");
            status = "starting...";
            try {
                rootContext.execute(new Executable2() {
                    @Override
                    public void execute(Context context) {
                        sessionManager.open();
                        try {
                            doProcess(context);
                            status = "finished";
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
                status = "generating";
                altHash = converter.generate(new With<InputStream, String>() {
                    @Override
                    public String use(InputStream t) throws Exception {
                        status = "generator parsing";
                        String newFileHash = parser.parse(t, hashStore, blobStore);
                        return newFileHash;
                    }
                });
            } catch (Exception e) {
                status = "conversion failed";
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

        @Override
        public String getPrimaryFileHash() {
            return primaryFileHash;
        }

        @Override
        public FormatSpec getFormatSpec() {
            return formatSpec;
        }
    }

    public boolean isEnableMetaData() {
        return enableMetaData;
    }

    public void setEnableMetaData(boolean enableMetaData) {
        this.enableMetaData = enableMetaData;
    }
}
