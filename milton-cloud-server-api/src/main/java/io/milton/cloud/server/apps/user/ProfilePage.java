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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.alt.AltFormatGenerator;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.Repository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;

/**
 * User's own profile page, for use within a website
 *
 * @author brad
 */
public class ProfilePage extends TemplatedHtmlPage implements PostableResource, PutableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProfilePage.class);
    public static final String PROFILE_PIC_CHILD = "pic";
    public static final String PICS_REPO_NAME = "ProfilePics";
    public static final long MAX_SIZE = 10000000l;
    private JsonResult jsonResult;

    public ProfilePage(String name, CommonCollectionResource parent) {
        super(name, parent, "user/profile", "Profile");
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("nickName")) {
            try {
                _(DataBinder.class).populate(p, parameters);
                session.save(p);
                tx.commit();
                jsonResult = new JsonResult(true);
            } catch (Exception ex) {
                log.error("exception: " + p.getId(), ex);
                jsonResult = new JsonResult(false, ex.getMessage());
            }
        } else if (parameters.containsKey("password")) {
            String newPassword = parameters.get("password");
            _(PasswordManager.class).setPassword(p, newPassword);
            jsonResult = new JsonResult(true);
            tx.commit();
        }
        return null;
    }

    /**
     * Intended to support PUT via ajax gateway, for compatibility with upload
     * js library.
     *
     * @param newName
     * @param inputStream
     * @param length
     * @param contentType
     * @return
     * @throws IOException
     * @throws ConflictException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Repository r = findProfilePics(p);
        Branch b = r.trunk(SessionManager.session());
        Commit head = b.getHead();
        if (head == null) {
        }
        HashStore hashStore = _(HashStore.class);
        BlobStore blobStore = _(BlobStore.class);
        DataSession dataSession = new DataSession(b, SessionManager.session(), hashStore, blobStore, _(CurrentDateService.class));
        DirectoryNode dir = dataSession.getRootDataNode();

        log.info("process file: " + newName + " size: " + length);
        if (length > MAX_SIZE) {
            throw new RuntimeException("File size is too big. Please select a file less then 10Mb");
        }
        Parser parser = new Parser();
        String fileHash = parser.parse(inputStream, hashStore, blobStore);
        log.info("saved inputstream with hash: " + fileHash);
        newName = findName(newName, dir);
        log.info("newName: " + newName);
        try {
            fileHash = _(AltFormatGenerator.class).generateProfileImage(fileHash, newName);
        } catch (Exception ex) {
            log.error("exception generating profile, will use the given file instead", ex);
        }
        log.info("generated thumb with hash: " + fileHash);
        dir.addFile(newName, fileHash);
        p.setPhotoHash(fileHash);
        session.save(p);
        tx.commit();
        return new ProfilePicResource(PROFILE_PIC_CHILD);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }

    public Profile getProfile() {
        return _(SpliffySecurityManager.class).getCurrentUser();
    }

    public String getPhotoHref() {
        Profile p = getProfile();
        if (p.getPhotoHash() != null && p.getPhotoHash().length() > 0) {
            return "/_hashes/files/" + p.getPhotoHash();
        } else {
            return "/templates/apps/user/profile.png";
        }
    }


    private Repository findProfilePics(Profile p) {
        Repository r = p.repository(PICS_REPO_NAME);
        if (r == null) {
            r = p.createRepository(PICS_REPO_NAME, p, SessionManager.session());
        }
        return r;
    }

    private String findName(String baseName, DataSession.DirectoryNode dir) {
        if (baseName == null || baseName.length() == 0) {
            baseName = "profile";
        } else {
            if (baseName.contains("\\")) {
                baseName = baseName.substring(baseName.lastIndexOf("\\"));
            }
        }
        String candidateName = baseName;
        int cnt = 1;
        while (contains(dir, candidateName)) {
            candidateName = baseName + cnt++;
        }
        return candidateName;
    }

    private boolean contains(DataSession.DirectoryNode dir, String name) {
        for (DataNode n : dir) {
            if (n.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        if (childName.equals(PROFILE_PIC_CHILD)) {
            return new ProfilePicResource(name);
        }
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }

    public class ProfilePicResource implements GetableResource, DigestResource {

        private final String name;
        private Fanout fanout;
        private boolean loaded;

        public ProfilePicResource(String name) {
            this.name = name;
        }
        
        @Override
        public Long getContentLength() {
            return fanout().getActualContentLength();
        }        
        
        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            Combiner combiner = new Combiner();
            combiner.combine(fanout().getHashes(), _(HashStore.class), _(BlobStore.class), out);
        }        
        
        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            if (fanout() == null) {
                return "/templates/apps/users/profile.png";
            } else {
                return null;
            }
        }        
        
        private Fanout fanout() {
            if( loaded ) {
                return fanout;
            }
            loaded = true;
            String hash = getProfile().getPhotoHash();
            if (hash != null) {
                fanout = _(HashStore.class).getFileFanout(hash);
            } else {
                fanout = null;
            }
            return fanout;
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
            return ProfilePage.this.authenticate(user, password);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return true;
        }

        @Override
        public String getRealm() {
            return ProfilePage.this.getRealm();
        }

        @Override
        public Date getModifiedDate() {
            return null;
        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
            return null;
        }

        @Override
        public String getContentType(String accepts) {
            return null;
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return ProfilePage.this.authenticate(digestRequest);
        }

        @Override
        public boolean isDigestAllowed() {
            return ProfilePage.this.isDigestAllowed();
        }
    }
}
