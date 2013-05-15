/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.alt;

import io.milton.cloud.server.db.HlsPrimary;
import io.milton.cloud.server.db.HlsProgram;
import io.milton.cloud.server.web.HashResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.alt.AltFormatGenerator.HlsGenerateJob;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.utils.SessionManager;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class HlsPrimaryPlayListResource implements GetableResource, DigestResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HlsPrimaryPlayListResource.class);
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private final FormatSpec formatSpec;
    private final HashResource rPrimary;
    private final String name;
    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final AltFormatGenerator altFormatGenerator;

    public HlsPrimaryPlayListResource(FormatSpec formatSpec, HashResource rPrimary, String name, HashResource hr, BlobStore blobStore, HashStore hashStore, AltFormatGenerator altFormatGenerator) {
        this.formatSpec = formatSpec;
        this.altFormatGenerator = altFormatGenerator;
        this.rPrimary = rPrimary;
        this.name = name;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        Session session = SessionManager.session();
        Transaction tx = SessionManager.beginTx();
        HlsPrimary p = HlsPrimary.findByHash(rPrimary.getHash(), session);
        if( p != null && params.containsKey("force")) {
            p.delete(session);
            session.flush();
            tx.commit();
            session.clear();
            p = HlsPrimary.findByHash(rPrimary.getHash(), session);
        }
        if (p == null || p.getPrograms() == null || p.getPrograms().isEmpty()) {
            log.info("No primary, will attempt to create it");
            // need to create a job
            AltFormatGenerator.GenerateJob generateJob = altFormatGenerator.getOrEnqueueJob(rPrimary.getHash(), rPrimary.getName(), formatSpec);
            HlsGenerateJob job = null;
            if (generateJob instanceof HlsGenerateJob) {
                job = (HlsGenerateJob) generateJob;
                log.info("Got a job: status=" + job.getStatus());
            } else {
                throw new RuntimeException("Couldnt get valid generate job: " + generateJob);
            }
            // just wait until the HLS Primary record appears
            int cnt = 0;
            while (p == null || p.getPrograms() == null || p.getPrograms().isEmpty()) {
                if (p != null) {
                    log.warn("Got a primary, but it has no programs. will try again - " + p.getPrograms());
                }
                log.info("wait.. " + cnt + " - " + p + " hash=" + rPrimary.getHash());
                cnt = checkTimer(cnt);
                session.clear();
                p = HlsPrimary.findByHash(rPrimary.getHash(), session);
            }
        }

        // ok, if we got here then by hook or by crook we have a HLS primary
        log.info("We have a primary. Programs=" + p.getPrograms().size());
        writeLine("#EXTM3U", out);
        for (HlsProgram prog : p.getPrograms()) {
            String line = "#EXT-X-STREAM-INF:PROGRAM-ID=1"; // we only do one program ID
            if (prog.getBandwidth() != null) {
                line += ",BANDWIDTH=" + prog.getBandwidth();
            }
            if (prog.getHeight() != null && prog.getWidth() != null) {
                String dim = prog.getWidth() + "x" + prog.getHeight();
                line += ",RESOLUTION=" + dim;
                writeLine(line, out);
                // now write the URL of the program playlist resource
                String href = "alt-" + dim + ".m3u8";
                writeLine(href, out);
            }
        }

    }

    private void writeLine(String line, OutputStream out) {

        try {
            out.write((line + "\n").getBytes(UTF8));
            System.out.println("writeline:" + line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 3l;
    }

    @Override
    public String getContentType(String accepts) {
        return "application/vnd.apple.mpegurl";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object authenticate(String user, String password) {
        return user;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;
    }

    @Override
    public String getRealm() {
        return _(SpliffySecurityManager.class).getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return digestRequest.getUser();
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    private int checkTimer(int cnt) {
        if (cnt > 20) {
            throw new RuntimeException("Timeout waiting for HLS Primary record to appear");
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return cnt + 1;
    }
}
