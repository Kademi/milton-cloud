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
import io.milton.cloud.server.db.HlsSegment;
import io.milton.cloud.server.web.HashResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class HlsProgramPlayListResource implements GetableResource, DigestResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HlsProgramPlayListResource.class);
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private final Dimension dimension;
    private final HashResource rPrimary;
    private final String name;
    private final BlobStore blobStore;
    private final HashStore hashStore;

    public HlsProgramPlayListResource(String name, HashResource hr, BlobStore blobStore, HashStore hashStore) {
        rPrimary = hr;
        // name is like alt-1280x720.m3u8
        this.name = name;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        String s = name.replace("alt-", "").replace(".m3u8", "");
        dimension = Dimension.parse(s);

    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        Session session = SessionManager.session();
        HlsProgram p = findProgram(session);
        if (p == null) {
            return;
        }
        writeLine("#EXTM3U", out);
        writeLine("#EXT-X-TARGETDURATION:" + p.getPrimaryPlaylist().getTargetDuration(), out); // note that targetduration must be greater then or equal to any segment duration
        writeLine("#EXT-X-VERSION:" + AvconvConverter.HLS_VERSION, out);
        //writeLine("#EXT-X-MEDIA-SEQUENCE:0", out);
//        if( p.getPrimaryPlaylist().isComplete()) {
//            writeLine("#EXT-X-PLAYLIST-TYPE:VOD", out); // VOD=video on demand. Playlist file must not change!! For live video use EVENT
//        } else {
//            writeLine("#EXT-X-PLAYLIST-TYPE:EVENT", out);
//        }
//
        for (HlsSegment segment : p.getSegments()) {
            writeLine("#EXTINF:" + segment.getDurationSecs() + ",", out);
            //String newSegmentUrl = "http://cdn1." + currentRootFolderService.getPrimaryDomain() + ":8080/_hashes/files/" + newSegmentHash;
            //String newSegmentUrl = "http://version1.idhealth.loopbackdns.com:8080/_hashes/files/" + newSegmentHash + ".ts";            
            String newSegmentUrl = segment.getSegmentHash() + ".ts";
            writeLine(newSegmentUrl, out);
        }

        if (p.getPrimaryPlaylist().isComplete()) {
            writeLine("#EXT-X-ENDLIST", out);
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

    private HlsProgram findProgram(Session session) {
        HlsPrimary p = HlsPrimary.findByHash(rPrimary.getHash(), session);
        if (p == null) {
            log.warn("Primary not found: " + rPrimary.getHash());
            return null;
        }
        if (p.getPrograms() == null || p.getPrograms().isEmpty()) {
            log.warn("no programs");
            return null;
        }
        for (HlsProgram prog : p.getPrograms()) {
            if (prog.getWidth() != null && prog.getHeight() != null) {
                if (dimension.getWidth() == prog.getWidth() && dimension.getHeight() == prog.getHeight()) {
                    return prog;
                }
            }
        }
        return null;
    }
}
