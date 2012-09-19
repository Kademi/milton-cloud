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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.manager.PasswordManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.alt.AltFormatGenerator;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DeletableResource;
import io.milton.resource.DigestResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageUserPage extends TemplatedHtmlPage implements GetableResource, PostableResource, PutableResource, CommonCollectionResource {

    private static final Logger log = LoggerFactory.getLogger(ManageUserPage.class);
    public static final String PROFILE_PIC_CHILD = "pic";
    public static final String PICS_REPO_NAME = "ProfilePics";
    public static final long MAX_SIZE = 10000000l;
    private Profile profile;
    private JsonResult jsonResult;
    private ResourceList children;
    private List<Organisation> orgSearchResults;

    public ManageUserPage(String name, Profile profile, CommonCollectionResource parent) {
        super(name, parent, "admin/profile", "Manage User");
        this.profile = profile;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        String sGroup = parameters.get("group");
        String orgId = parameters.get("orgId"); // to create the membership in

        if (parameters.containsKey("nickName")) {
            // is create or update
            boolean isNew = false;
            if (profile == null) {
                String nickName = parameters.get("nickName");
                if (sGroup == null) {
                    jsonResult = JsonResult.fieldError("group", "Please select a group for the new user");
                    return null;
                }
                String email = WebUtils.getParam(parameters, "email");
                Profile pExisting = Profile.find(email, session);
                if (pExisting != null) {
                    jsonResult = JsonResult.fieldError("password", "An existing user account was found with that email address.");
                    return null;
                }
                String nameToCreate = Profile.findAutoName(nickName, session);
                Date now = _(CurrentDateService.class).getNow();
                profile = new Profile();
                profile.setName(nameToCreate);
                profile.setCreatedDate(now);
                profile.setModifiedDate(now);
                isNew = true;
            }

            try {
                _(DataBinder.class).populate(profile, parameters);
                session.save(profile);
                if (sGroup != null) {
                    // We need a group and an org to create a membership                    
                    if (!addMembership(sGroup, orgId, session)) {
                        return null;
                    }
                }
                tx.commit();
                jsonResult = new JsonResult(true);
                if (isNew) {
                    System.out.println("parent path: " + parent.getPath() + " for " + parent.getClass());
                    String newHref = parent.getPath().child(profile.getId() + "").toString();
                    jsonResult.setNextHref(newHref);
                }
            } catch (Exception ex) {
                log.error("exception: " + profile.getId(), ex);
                jsonResult = new JsonResult(false, ex.getMessage());
            }
        } else if (parameters.containsKey("password")) {
            String newPassword = parameters.get("password");
            _(PasswordManager.class).setPassword(profile, newPassword);
            jsonResult = new JsonResult(true);
            tx.commit();
        } else if (parameters.containsKey("group")) { // create a new group membersip
            log.info("processForm: add membership: " + sGroup + " " + orgId);
            if (!addMembership(sGroup, orgId, session)) {
                return null;
            }
            tx.commit();
            jsonResult = new JsonResult(true);
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
        Profile p = getProfile();
        Repository r = findProfilePics(p);
        Branch b = r.getTrunk();
        Commit head = b.getHead();
        if (head == null) {
        }
        HashStore hashStore = _(HashStore.class);
        BlobStore blobStore = _(BlobStore.class);
        DataSession dataSession = new DataSession(b, SessionManager.session(), hashStore, blobStore, _(CurrentDateService.class));
        DataSession.DirectoryNode dir = dataSession.getRootDataNode();

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
            String orgSearch = params.get("orgSearch");
            if (orgSearch != null && orgSearch.length() > 0) {
                orgSearchResults = Organisation.search(orgSearch, getOrganisation(), SessionManager.session()); // find the given user in this organisation 
                log.info("results: " + orgSearchResults.size());
            } else {
                orgSearchResults = Collections.EMPTY_LIST;
            }

            super.sendContent(out, range, params, contentType);
        }
    }

    public List<Organisation> getOrgSearchResults() {
        return orgSearchResults;
    }

    public Profile getProfile() {
        return profile;
    }

    /**
     * Return only those memberships which are visible to this organisation
     *
     * @return
     */
    public List<GroupMembership> getMemberships() {
        List<GroupMembership> list = new ArrayList<>();
        if (profile != null) {
            if (profile.getMemberships() != null) {
                Organisation parentOrg = getOrganisation();
                for (GroupMembership m : profile.getMemberships()) {
                    if (m.getWithinOrg().isWithin(parentOrg)) {
                        list.add(m);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    public String getPhotoHref() {
        Profile p = getProfile();
        if (p != null && p.getPhotoHash() != null && p.getPhotoHash().length() > 0) {
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
        for (DataSession.DataNode n : dir) {
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
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for (GroupMembership gm : getMemberships()) {
                MembershipResource m = new MembershipResource(gm);
                children.add(m);
            }
        }
        return children;
    }

    public List<Group> getGroups() {
        List<Group> groups = getOrganisation().getGroups();
        if (groups == null) {
            return Collections.EMPTY_LIST;
        }
        return groups;
    }

    /**
     * Returns true if successful, otherwise a validation error has occured
     *
     * @param sGroup
     * @return
     */
    private boolean addMembership(String sGroup, String orgId, Session session) {
        Group group = getOrganisation().group(sGroup, session);
        if (group == null) {
            jsonResult = JsonResult.fieldError("group", "Sorry, I couldnt find group: " + sGroup);
            return false;
        }
        if (orgId == null) {
            jsonResult = JsonResult.fieldError("orgId", "Please select an organisation");
            return false;
        }
        Organisation subOrg = Organisation.findByOrgId(orgId, session);
        if (subOrg == null) {
            jsonResult = JsonResult.fieldError("orgId", "Organisation not found: " + orgId);
            return false;
        }
        if (!subOrg.isWithin(getOrganisation())) {
            throw new RuntimeException("Selected org is not contained within this org. selected orgId=" + orgId + " this org: " + getOrganisation().getOrgId());
        }
        profile.addToGroup(group, subOrg, session);
        SignupLog.logSignup(null, getOrganisation(), profile, subOrg, group, SessionManager.session());
        return true;
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
                return "/templates/apps/user/profile.png";
            } else {
                return null;
            }
        }

        private Fanout fanout() {
            if (loaded) {
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
            return ManageUserPage.this.authenticate(user, password);
        }

        @Override
        public boolean authorise(Request request, Request.Method method, Auth auth) {
            return true;
        }

        @Override
        public String getRealm() {
            return ManageUserPage.this.getRealm();
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
            return ManageUserPage.this.authenticate(digestRequest);
        }

        @Override
        public boolean isDigestAllowed() {
            return ManageUserPage.this.isDigestAllowed();
        }
    }

    /**
     * Represents a membership of a user with a group
     *
     */
    public class MembershipResource extends AbstractResource implements DeletableResource {

        private GroupMembership membership;

        public MembershipResource(GroupMembership membership) {
            this.membership = membership;
        }

        public String getGroupName() {
            return membership.getGroupEntity().getName();
        }

        public String getOrgName() {
            return membership.getWithinOrg().getFormattedName();
        }

        @Override
        public CommonCollectionResource getParent() {
            return ManageUserPage.this;
        }

        @Override
        public Organisation getOrganisation() {
            return ManageUserPage.this.getOrganisation();
        }

        @Override
        public Priviledge getRequiredPostPriviledge(Request request) {
            return null;
        }

        @Override
        public String getName() {
            return membership.getGroupEntity().getName() + "-" + membership.getWithinOrg().getOrgId();
        }

        @Override
        public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            membership.delete(session);
            tx.commit();
        }

        @Override
        public boolean is(String type) {
            if (type.equals("membership")) {
                return true;
            }
            return super.is(type);
        }
    }
}
