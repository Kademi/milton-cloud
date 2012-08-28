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

import io.milton.cloud.server.db.AltFormat;
import io.milton.cloud.server.db.MediaMetaData;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.alt.AltFormatGenerator.GenerateJob;
import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.BufferingControlResource;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

/**
 * Provides access to alternative formats for a given file.
 *
 * for example
 *
 * @author brad
 */
public class AltFormatResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AltFormatResourceFactory.class);
    private final ResourceFactory wrapped;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final AltFormatGenerator altFormatGenerator;

    public AltFormatResourceFactory(ResourceFactory wrapped, HashStore hashStore, BlobStore blobStore, AltFormatGenerator altFormatGenerator) {
        this.wrapped = wrapped;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.altFormatGenerator = altFormatGenerator;
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        Path p = Path.path(sPath);
        if (p.getName().startsWith("alt-")) {
            Resource r = wrapped.getResource(host, p.getParent().toString());
            if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                String sourceHash = fr.getHash();
                String formatName = p.getName().replace("alt-", "");
                AltFormat f = AltFormat.find(sourceHash, formatName, SessionManager.session());
                FormatSpec format = altFormatGenerator.findFormat(formatName);
                if (f != null) {                    
                    AltFormatResource alt = new AltFormatResource((FileResource) r, p.getName(), f, format);
                    log.info("getResource: created resource for format: " + format + " - " + alt);
                    return alt;
                } else {
                    log.warn("getResource: pre-generated alt format not found: " + sourceHash + " - " + p.getName());
                    // if the format is valid then create a resource which will generate on demand                                        
                    if (format != null) {                        
                        AltFormatResource alt = new AltFormatResource((FileResource) r, p.getName(), format);
                        log.info("getResource: created resource for format: " + format + " - " + alt);
                        return alt;
                    } else {
                        log.warn("getResource: unrecognised format: " + formatName);
                    }
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return wrapped.getResource(host, sPath);
        }
    }

    /**
     * Used the hash of the resource and the content type embedded in the name
     * to locate an alternative representation of the resource
     */
    public class AltFormatResource implements GetableResource, DigestResource, BufferingControlResource {

        private final FileResource rPrimary;
        private final String name;
        private final FormatSpec formatSpec;
        private AltFormat altFormat;
        private Fanout fanout;
        private boolean doneFanoutLookup;

        public AltFormatResource(FileResource rPrimary, String name, AltFormat altFormat, FormatSpec formatSpec) {
            this.rPrimary = rPrimary;
            this.name = name;
            this.altFormat = altFormat;
            this.formatSpec = formatSpec;
        }

        public AltFormatResource(FileResource rPrimary, String name, FormatSpec formatSpec) {
            this.rPrimary = rPrimary;
            this.name = name;
            this.altFormat = null;
            this.formatSpec = formatSpec;
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {            
            log.info("sendContent");
            MediaMetaData mmd = MediaMetaData.find(rPrimary.getHash(), SessionManager.session());
            if( mmd != null ) {
                Integer durationSecs = mmd.getDurationSecs();
                if( durationSecs != null ) {
                    Response resp = HttpManager.response();
                    if( resp != null ) {
                        System.out.println("set duration header: " + durationSecs);
                        resp.setNonStandardHeader("X-Content-Duration", durationSecs.toString());
                    }
                }
            } else {
                System.out.println("no metadata for: " + rPrimary.getHash());
            }
            try {
                boolean force = params.containsKey("force");
                if (altFormat == null || force) {
                    // hack start
                    log.info("sendContent: will generate");
                    if( params.containsKey("args")) {
                        List<String> args = new ArrayList<>();
                        for( String s : params.get("args").split(",")) {
                            args.add(s);
                        }
                        String[] arr = new String[args.size()];
                        args.toArray(arr);
                        formatSpec.setConverterArgs(arr);
                        System.out.println("set args: " + arr);                        
                    }
                    
                    // hack end
                    System.out.println("generate: " + getName());
                    GenerateJob j = altFormatGenerator.getOrEnqueueJob(rPrimary.getHash(), rPrimary.getName(), formatSpec);
                    System.out.println("got job: " + j);

                    // Wait until the file exists
                    int cnt = 0;
                    System.out.println("check if exists..." );
                    while (!j.getDestFile().exists() && !j.done()) {
                        cnt++;
                        System.out.println("sleep..." + cnt + " .. " + j.getDestFile().exists() + " - " + j.done());
                        doSleep(cnt++, 200, 70);
                    }
                    System.out.println("finished sleepy check");
                    if (!j.getDestFile().exists()) {
                        throw new RuntimeException("Job did not create a destination file: " + j.getDestFile().getAbsolutePath());
                    }
                    System.out.println("use dest file: " + j.getDestFile().getAbsolutePath() + " size: " + j.getDestFile().length());
                    
                    FileInputStream fin = new FileInputStream(j.getDestFile());
                    byte[] buf = new byte[1024];
                    System.out.println("send file...");
                    // Read the file until the job is done, or we run out of bytes
                    int s = fin.read(buf);
                    System.out.println("send file... " + s);
                    long bytes = 0;
                    while (!j.done() || s > 0) {
                        if (s < 0) { // no bytes available, but job is not done, so wait
                            System.out.println("sleep...");
                            doSleep(100);
                        } else {
                            System.out.println("write bytes: " + s);
                            bytes += s;
                            out.write(buf, 0, s);
                        }
                        s = fin.read(buf);
                    }
                    System.out.println("finished sending file: " + bytes);
                } else {
                    log.info("sendContent: using existing");
                    Combiner combiner = new Combiner();
                    List<String> fanoutCrcs = getFanout().getHashes();
                    combiner.combine(fanoutCrcs, hashStore, blobStore, out);
                    out.flush();
                }
            } catch (Throwable e) {
                log.error("Exception sending content", e);
                throw new IOException("Exception sending content");
            }
        }

        @Override
        public Boolean isBufferingRequired() {
            return false;
        }

        private Fanout getFanout() {
            if (!doneFanoutLookup) {
                doneFanoutLookup = true;
                if (altFormat != null && !HttpManager.request().getParams().containsKey("force")) {
                    fanout = hashStore.getFileFanout(altFormat.getAltHash());
                }
            }
            return fanout;
        }
//
//        private AltFormat getAltFormat() {
//            if (altFormat == null) {
//                Transaction tx = SessionManager.session().beginTransaction();
//                try {
//                    altFormat = altFormatGenerator.generate(formatSpec, r);
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//                tx.commit();
//            }
//            return altFormat;
//        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
            return 60 * 60 * 24l;
        }

        @Override
        public String getContentType(String accepts) {
            String canProvide = ContentTypeUtils.findContentTypes(name);
            String type = ContentTypeUtils.findAcceptableContentType(canProvide, accepts);
            System.out.println("content type: " + type);
            return type;
        }

        @Override
        public Long getContentLength() {            
            if (getFanout() != null) {
                Long l = getFanout().getActualContentLength();
                log.info("getContentLength: " + l);
                return l;
            }
            log.info("getContentLength: no content length");
            return null;
        }

        @Override
        public String getUniqueId() {
            if (altFormat != null) {
                return altFormat.getAltHash() + "";
            } else {
                return null;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object authenticate(String user, String password) {
            return rPrimary.authenticate(user, password);
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return rPrimary.authenticate(digestRequest);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return rPrimary.authorise(request, method, auth);
        }

        @Override
        public String getRealm() {
            return rPrimary.getRealm();
        }

        @Override
        public Date getModifiedDate() {
            return rPrimary.getModifiedDate();
        }

        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            return null;
        }

        @Override
        public boolean isDigestAllowed() {
            return rPrimary.isDigestAllowed();
        }

        private void doSleep(long sleepMillis) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void doSleep(int cnt, long sleepMillis, int maxCnt) {
            if (cnt > maxCnt) {
                throw new RuntimeException("Timeout");
            }
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
