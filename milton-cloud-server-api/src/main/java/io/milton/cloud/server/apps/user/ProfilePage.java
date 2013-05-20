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
import io.milton.cloud.server.DataSessionManager;
import io.milton.cloud.server.db.OptIn;
import io.milton.cloud.server.db.OptInLog;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ExtraField;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NodeChildUtils;
import io.milton.cloud.server.web.OrgData;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.alt.AltFormatGenerator;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import static io.milton.context.RequestContext._;
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

import io.milton.http.Auth;
import io.milton.http.HttpManager;
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
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.NvPair;
import io.milton.vfs.db.NvSet;
import io.milton.vfs.db.OrgType;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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
    
    public static Map<ExtraField,String> getExtraFields(Organisation thisOrg) {
        Map<ExtraField,String> mapOfFields = new LinkedHashMap<>();
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Set<String> fieldNames = new HashSet<>();
        if (p.getMemberships() != null) {
            for (GroupMembership gm : p.getMemberships()) {
                if (gm.getWithinOrg().isWithin(thisOrg)) {
                    NvSet fieldsetMeta = gm.getGroupEntity().getFieldset();
                    if (fieldsetMeta != null) {
                        NvSet fieldsetValues = gm.getFields();
                        Map<String, String> mapOfValues = fieldsetValues.toMap();
                        for (NvPair nvpMeta : fieldsetMeta.getNvPairs()) {
                            ExtraField fieldMeta = ExtraField.parse(nvpMeta.getName(), nvpMeta.getPropValue());
                            if (!fieldNames.contains(fieldMeta.getName()) || fieldMeta.isRequired()) {
                                fieldNames.add(nvpMeta.getName());
                                ExtraField xf = ExtraField.parse(nvpMeta.getName(), nvpMeta.getPropValue());
                                String value = mapOfValues.get(xf.getName());
                                if( value == null ) {
                                    value = ""; // otherwise velocity doesnt set variable
                                }
                                mapOfFields.put(xf, value);
                            }
                        }

                    }

                }
            }
        }

        return mapOfFields;
    }    
    
    
    public static final String PROFILE_PIC_CHILD = "pic";
    public static final String PICS_REPO_NAME = "ProfilePics";
    public static final long MAX_SIZE = 10000000l;
    private JsonResult jsonResult;
    private Set<OptIn> optIns;
    private List<Organisation> orgSearchResults;
    private boolean doneOrgSearch;

    public ProfilePage(String name, CommonCollectionResource parent) {
        super(name, parent, "user/profile", "Profile");
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("changeMemberOrg")) {
            updateMembershipOrg(parameters, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Updated membership org");
        } else if (parameters.containsKey("enableOptin")) {
            boolean enableOptin = WebUtils.getParamAsBool(parameters, "enableOptin");
            String optinGroupName = WebUtils.getParam(parameters, "group");
            setOptin(enableOptin, optinGroupName, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Set optin: " + optinGroupName + " = " + enableOptin);

        } else if (parameters.containsKey("removeMembership")) {
            int membershipId = WebUtils.getParamAsInteger(parameters, "removeMembership");
            removeMembership(membershipId, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Deleted membership");
        } else if (parameters.containsKey("nickName")) {
            // This is the main form
            try {
                String oldEmail = p.getEmail();
                String newEmail = parameters.get("email");
                if (newEmail != null) {
                    newEmail = newEmail.trim();
                }
                if (oldEmail == null || (newEmail != null && !newEmail.equals(oldEmail))) {
                    Profile someOtherUser = Profile.findByEmail(newEmail, getOrganisation(), session);
                    if (someOtherUser != null) {
                        log.warn("Found another user with that email: " + newEmail);
                        jsonResult = JsonResult.fieldError("email", "There is another user account registered with that email");
                        return null;
                    }
                }
                _(DataBinder.class).populate(p, parameters);
                storeExtraFields(parameters, p, session);

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
        Branch b = r.getTrunk();
        Commit head = b.getHead();
        if (head == null) {
        }
        HashStore hashStore = _(HashStore.class);
        BlobStore blobStore = _(BlobStore.class);

        DataSession dataSession = _(DataSessionManager.class).get(b);
        DirectoryNode dir = dataSession.getRootDataNode();

        log.info("process file: " + newName + " size: " + length);
        if (length > MAX_SIZE) {
            throw new RuntimeException("File size is too big. Please select a file less then 10Mb");
        }
        Parser parser = new Parser();
        String fileHash = parser.parse(inputStream, hashStore, blobStore);
        log.info("saved inputstream with hash: " + fileHash);
        newName = NodeChildUtils.findName(newName, "profile", dir);
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
        System.out.println("contentType: " + contentType);
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            if (params.containsKey("changeMemberOrg")) {
                if (contentType.equals("application/json")) {
                    List<OrgData> orgs = new ArrayList<>();

                    for (Organisation org : getOrgSearchResults()) {
                        OrgData d = new OrgData(org);
                        orgs.add(d);
                    }
                    jsonResult = new JsonResult(true, "Done org search");
                    jsonResult.setData(orgs);
                    jsonResult.write(out);

                } else {
                    _(HtmlTemplater.class).writePage("user/changeOrg", this, params, out);
                }
            } else {
                super.sendContent(out, range, params, contentType);
            }
        }
    }

    @Override
    public String getContentType(String accepts) {
        if (accepts.contains("application/json") || accepts.contains("text/javascript")) {
            return "application/json";
        }
        return "text/html";
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

    /**
     * Get all GroupMembership objects for the current user, which are NOT
     * Opt-in groups. Opt-ins are managed seperately from groups
     *
     * @return
     */
    public List<GroupMembership> getMemberships() {
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Organisation thisOrg = getOrganisation();
        List<GroupMembership> list = new ArrayList<>();
        Set<Group> optinGroups = new HashSet<>();
        for (OptIn optin : getOptins()) {
            optinGroups.add(optin.getOptinGroup());
        }
        if (p.getMemberships() != null) {
            for (GroupMembership gm : p.getMemberships()) {
                if (gm.getWithinOrg().isWithin(thisOrg)) {
                    // if user has a membership to an optin group we should not show this as a normal membership
                    if (!optinGroups.contains(gm.getGroupEntity())) {
                        list.add(gm);
                    }
                }
            }
        }
        return list;
    }

    public GroupMembership membership(long id) {
        for (GroupMembership gm : getMemberships()) {
            if (gm.getId() == id) {
                return gm;
            }
        }
        return null;
    }

    public Set<OptIn> getOptins() {
        if (optIns == null) {
            optIns = new HashSet<>();
            Profile p = _(SpliffySecurityManager.class).getCurrentUser();
            Organisation thisOrg = getOrganisation();
            if (p.getMemberships() != null) {
                for (GroupMembership gm : p.getMemberships()) {
                    if (gm.getWithinOrg().isWithin(thisOrg)) {
                        for (OptIn optin : OptIn.findForGroup(gm.getGroupEntity(), SessionManager.session())) {
                            optIns.add(optin);
                        }
                    }
                }
            }
        }
        return optIns;
    }

    private void removeMembership(int membershipId, Session session) {
        GroupMembership found = null;
        for (GroupMembership gm : getMemberships()) {
            if (gm.getId() == membershipId) {
                found = gm;
                break;
            }
        }
        if (found != null) {
            Profile p = _(SpliffySecurityManager.class).getCurrentUser();
            p.removeMembership(found.getGroupEntity(), session);
        }
    }

    private void setOptin(boolean enableOptin, String optinGroupName, Session session) {
        Group foundGroup = null;
        for (OptIn optin : getOptins()) {
            if (optin.getOptinGroup().getName().equals(optinGroupName)) {
                foundGroup = optin.getOptinGroup();;
                break;
            }
        }
        if (foundGroup != null) {
            Organisation thisOrg = getOrganisation();
            Profile p = _(SpliffySecurityManager.class).getCurrentUser();
            if (enableOptin) {
                p.addToGroup(foundGroup, thisOrg, session);
                String sourceIp = "unknown";
                if (HttpManager.request() != null) {
                    sourceIp = HttpManager.request().getFromAddress();
                }
                OptInLog.create(p, sourceIp, foundGroup, sourceIp, session);

                Website w = WebUtils.getWebsite(this);
                SignupLog.logSignup(w, p, thisOrg, foundGroup, SessionManager.session());
            } else {
                p.removeMembership(foundGroup, session);
            }
        }
    }

    /**
     * Lazy loaded
     */
    private Map<ExtraField,String> extraFields;
    
    /**
     * Iterate over all groups that this user is a member of and which are
     * contained within this organisation.
     *
     * For each such group build a list of all the extra fields specified for
     * that group
     *
     * If a user is a member of multiple groups which have fields of the same
     * name, any required field will have preference over any non-required
     * field. But there is no other ordering or preferece.
     *
     * @return
     */
    public Map<ExtraField,String> getExtraFields() {
        if( extraFields == null ) {
            extraFields = getExtraFields(getOrganisation());
        }
        return extraFields;
    }

    private void storeExtraFields(Map<String, String> parameters, Profile p, Session session) {
        Date now = _(CurrentDateService.class).getNow();
        Organisation thisOrg = getOrganisation();
        if (p.getMemberships() != null) {
            for (GroupMembership gm : p.getMemberships()) {
                if (gm.getWithinOrg().isWithin(thisOrg)) {
                    // Create a new fieldset, then we will compare it with the old one and only
                    // save it if it is different (ie dirty)
                    NvSet oldFieldset = gm.getFields();
                    NvSet newFieldset = NvSet.create(oldFieldset);

                    for (NvPair nvpMeta : gm.getGroupEntity().getFieldMetaData()) {
                        String val;
                        if (parameters.containsKey(nvpMeta.getName())) {
                            ExtraField field = ExtraField.parse(nvpMeta.getName(), nvpMeta.getPropValue());
                            val = WebUtils.getParam(parameters, field.getName());
                        } else {
                            val = oldFieldset.get(nvpMeta.getName());
                        }
                        newFieldset.addPair(nvpMeta.getName(), val);
                    }
                    if (newFieldset.isDirty(oldFieldset)) {
                        gm.setFields(newFieldset);
                        gm.setModifiedDate(now);
                        session.save(newFieldset);
                        for (NvPair nvp : newFieldset.getNvPairs()) {
                            session.save(nvp);
                        }
                        session.save(gm);
                        session.flush();
                    }
                }
            }
        }
    }

    public List<Organisation> getOrgSearchResults() {
        if (orgSearchResults != null) {
            return orgSearchResults;
        }
        Map<String, String> params = HttpManager.request().getParams();
        String q = WebUtils.getParam(params, "orgSearchQuery");
        int changeMemberOrgId = WebUtils.getParamAsInteger(params, "changeMemberOrg");
        GroupMembership membership = membership(changeMemberOrgId);
        if (q != null && q.length() > 0) {
            doneOrgSearch = true;
            Organisation rootSearchOrg = getOrganisation();
            OrgType regoOrgType = membership.getGroupEntity().getRegoOrgType();
            orgSearchResults = Organisation.search(q, rootSearchOrg, regoOrgType, SessionManager.session());
        } else {
            orgSearchResults = Collections.EMPTY_LIST;
        }
        return orgSearchResults;
    }

    public boolean isDoneOrgSearch() {
        return doneOrgSearch;
    }
    
    

    private void updateMembershipOrg(Map<String, String> params, Session session) {
        long changeMemberOrgId = WebUtils.getParamAsLong(params, "changeMemberOrg");
        long newOrgId = WebUtils.getParamAsLong(params, "orgId");
        GroupMembership membership = membership(changeMemberOrgId);        
        
        if( !membership.getGroupEntity().isOpenGroup() ) {
            throw new RuntimeException("Can only update membership on an open group");
        }
        
        Organisation newOrg = Organisation.get(newOrgId, session);
        if( newOrg == null ) {
            throw new RuntimeException("Could not find organisation with ID=" + newOrgId);
        }
        if (newOrg.isWithin(getOrganisation())) {
                        
            membership.setWithinOrg(newOrg);

            membership.updateSubordinates(session);
            
            session.save(membership);
        } else {
            throw new RuntimeException("Invalid organisation: " + newOrgId);
        }
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
            Profile p = getProfile();
            if (p != null) {
                String hash = p.getPhotoHash();
                if (hash != null) {
                    fanout = _(HashStore.class).getFileFanout(hash);
                } else {
                    fanout = null;
                }
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
