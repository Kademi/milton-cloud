/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.admin;

import io.milton.annotations.BeanProperty;
import io.milton.annotations.BeanPropertyResource;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.db.OptIn;
import io.milton.cloud.server.event.GroupDeletedEvent;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.context.ClassNotInContextException;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.event.EventManager;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.resource.CollectionResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.DeletableResource;
import io.milton.resource.MoveableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.sync.event.EventUtils;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.lang.reflect.InvocationTargetException;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton", enableByDefault = false)
public class ManageGroupFolder extends AbstractResource implements PostableResource, CommonCollectionResource, GetableResource, CollectionResource, DeletableResource, PropertySourcePatchSetter.CommitableResource, MoveableResource, DeletableCollectionResource {

    private static final Logger log = LoggerFactory.getLogger(ManageGroupFolder.class);
    private final Group group;
    private final CommonCollectionResource parent;
    private ResourceList children;
    private JsonResult jsonResult;

    public ManageGroupFolder(CommonCollectionResource parent, Group group) {
        this.parent = parent;
        this.group = group;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (parameters.containsKey("addFieldName")) {
            String addFieldName = WebUtils.getRawParam(parameters, "addFieldName");
            String addFieldValue = WebUtils.getRawParam(parameters, "addFieldValue");
            addField(addFieldName, addFieldValue, session);
            tx.commit();
            jsonResult = new JsonResult(true);
        } else if (parameters.containsKey("removeFieldName")) {
            String addFieldName = WebUtils.getRawParam(parameters, "removeFieldName");
            removeField(addFieldName, session);
            tx.commit();
            jsonResult = new JsonResult(true);
        } else if (parameters.containsKey("role")) {
            String appliesToType = WebUtils.getRawParam(parameters, "appliesToType");
            String appliesTo = WebUtils.getRawParam(parameters, "appliesTo");
            String role = WebUtils.getRawParam(parameters, "role");
            if (appliesToType == null) {
                jsonResult = JsonResult.fieldError("appliesToType", "Please select a appliesToType");
                return null;
            } else if (role == null) {
                jsonResult = JsonResult.fieldError("role", "Please select a role");
                return null;
            }
            GroupRole gr;
            switch (appliesToType) {
                case "selectRepo":
                    Repository r = getOrganisation().repository(appliesTo);
                    if (r == null) {
                        jsonResult = JsonResult.fieldError("appliesTo", "Could not locate repository: " + appliesTo);
                        return null;
                    } else {
                        gr = group.grantRole(r, role, session);
                    }
                    break;
                case "selectOrg":
                    Organisation org;
                    Long appliesToId = Long.parseLong(appliesTo);
                    org = Organisation.get(appliesToId, session);

                    if (org == null) {
                        jsonResult = JsonResult.fieldError("appliesTo", "Could not locate child organistion: " + appliesTo);
                        return null;
                    } else if(!org.isWithin(getOrganisation())) {
                        jsonResult = JsonResult.fieldError("appliesTo", "Selected organistion is not within parent: " + appliesTo);
                        return null;
                        
                    } else {
                        gr = group.grantRole(org, role, session);
                    }
                    break;
                default:
                    gr = group.grantRole(role, session);
                    break;
            }
            if (gr != null) {
                tx.commit();
                ManageGroupRolePage grPage = new ManageGroupRolePage(this, gr);
                jsonResult = new JsonResult(true, "Added role", grPage.getHref());
            } else {
                jsonResult = new JsonResult(false, "That role already exists");
            }
        } else if (parameters.containsKey("regoMode")) {
            try {
                String sRegoMode = WebUtils.getRawParam(parameters, "regoMode");
                group.setRegistrationMode(sRegoMode);
                String sOrgType = WebUtils.getRawParam(parameters, "orgType");
                OrgType orgType = null;
                if (sOrgType != null) {
                    orgType = getOrganisation().orgType(sOrgType);
                    if (orgType == null) {
                        throw new RuntimeException("Couldnt find orgType: " + sOrgType);
                    }
                } else {
                    System.out.println("no org type");
                }
                group.setRegoOrgType(orgType, session);
                System.out.println("set ot: " + group.getRegoOrgType());
                String sRootOrg = WebUtils.getRawParam(parameters, "sRootRegoOrg");
                if (sRootOrg != null) {
                    Organisation org = getOrganisation().childOrg(sRootOrg, session);
                    group.setRootRegoOrg(org);
                } else {
                    group.setRootRegoOrg(null);
                }

                // update opt-ins
                String sOptinGroups = WebUtils.getRawParam(parameters, "optinGroup");
                List<OptIn> toRemove = OptIn.findForGroup(group, session);
                if (sOptinGroups != null) {
                    for (String groupName : sOptinGroups.split(",")) {
                        groupName = groupName.trim();
                        if (groupName.length() > 0) {
                            Group optinGroup = getOrganisation().group(groupName, session);
                            if (optinGroup == null || optinGroup == group) {
                                // ignore
                            } else {
                                OptIn o = OptIn.findOptin(toRemove, optinGroup);
                                if (o == null) {
                                    o = OptIn.create(group, optinGroup);
                                } else {

                                    toRemove.remove(o); // found it, so remove it from list. We will delete whats left
                                }
                                String desc = WebUtils.getRawParam(parameters, "optin" + groupName + "_Desc");
                                if (desc == null) {
                                    desc = "Would you like to receive communications for " + groupName;
                                }
                                o.setMessage(desc);
                                session.save(o);
                            }
                        }
                    }
                }
                for (OptIn o : toRemove) {
                    session.delete(o);
                }

                _(DataBinder.class).populate(group, parameters);
                session.save(group);
                tx.commit();
                jsonResult = new JsonResult(true, "Saved group");
            } catch (ClassNotInContextException | IllegalAccessException | InvocationTargetException ex) {
                tx.rollback();
                jsonResult = new JsonResult(false, ex.getMessage());
            }
        } else if (parameters.containsKey("sourceGroup")) {
            String sSourceGroup = WebUtils.getRawParam(parameters, "sourceGroup");
            Group sourceGroup = getOrganisation().group(sSourceGroup, session);
            if (sourceGroup == null) {
                jsonResult = new JsonResult(false, "Couldnt find group: " + sSourceGroup);
            } else {
                if (sourceGroup == group) {
                    jsonResult = new JsonResult(false, "The source group is the same as the destination group");
                } else {
                    int count = 0;
                    if (sourceGroup.getGroupMemberships() != null) {
                        for (GroupMembership m : sourceGroup.getGroupMemberships()) {
                            count++;
                            m.getMember().getOrCreateGroupMembership(group, m.getWithinOrg(), session, null);
                        }
                    }
                    session.save(group);
                    tx.commit();
                    jsonResult = new JsonResult(true, "Added members: " + count);

                }
            }
        }

        return null;
    }

    public List<OptIn> getOptins() {
        return OptIn.findForGroup(group, SessionManager.session());
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (group.getGroupRoles() != null) {
                for (GroupRole gr : group.getGroupRoles()) {
                    ManageGroupRolePage p = new ManageGroupRolePage(this, gr);
                    children.add(p);
                }
            }
        }
        return children;
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        EventUtils.fireQuietly(_(EventManager.class), new GroupDeletedEvent(group));
        group.delete(session);
        tx.commit();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_ACL;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult == null) {
            _(HtmlTemplater.class).writePage("admin", "admin/manageGroupRegoMode", this, params, out);
//            Map<String, String> map = new HashMap<>();
//            map.put("regoMode", group.getRegistrationMode());
//            String sOrgType = null;
//            if (group.getRegoOrgType() != null) {
//                sOrgType = group.getRegoOrgType().getName();
//            }
//            map.put("orgType", sOrgType);
//            String sRootRegoOrgId = group.getRootRegoOrg() == null ? "" : group.getRootRegoOrg().getOrgId();
//            map.put("rootRegoOrg", sRootRegoOrgId);
//            jsonResult = new JsonResult(true);
//            jsonResult.setData(map);
        } else {
            jsonResult.write(out);
        }
    }

    public List<Group> getGroups() {
        return Group.findByOrg(getOrganisation(), SessionManager.session());
    }

    public String getTitle() {
        return "Manage group: " + group.getName();
    }

    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @BeanProperty(value = true)
    @Override
    public String getName() {
        return group.getName();
    }

    public void setName(String s) {
        group.setName(s);
    }

    @BeanProperty(value = true)
    public String getRegoMode() {
        return group.getRegistrationMode();
    }

    public void setRegoMode(String s) {
        group.setRegistrationMode(s);
    }

    public boolean isPublicSignup() {
        if (getRegoMode().equals(Group.REGO_MODE_ADMIN_REVIEW)) {
            return true;
        } else if (getRegoMode().equals(Group.REGO_MODE_OPEN)) {
            return true;
        }
        return false;
    }
    
    public String getRegoModeText() {
        if (getRegoMode().equals(Group.REGO_MODE_ADMIN_REVIEW)) {
            return "Administrator review";
        } else if (getRegoMode().equals(Group.REGO_MODE_CLOSED)) {
            return "Closed";
        } else if (getRegoMode().equals(Group.REGO_MODE_OPEN)) {
            return "Open";
        } else {
            return getRegoMode();
        }
    }

    @Override
    public Date getModifiedDate() {
        return group.getModifiedDate();
    }

    @Override
    public Date getCreateDate() {
        return group.getModifiedDate();
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean is(String type) {
        if (type.equals("group")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        if (childName.equals("members")) {
            return new ManageGroupMembersPage(this, childName);
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        session.save(group);
        tx.commit();
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (!(rDest instanceof ManageGroupsFolder)) {
            throw new ConflictException("Parent folder must be manage groups folder. Is a: " + rDest.getClass() + " with name: " + rDest.getName());
        }
        if( name.equals("users")) {
            throw new ConflictException("Cannot rename to reserved word 'users'");
        }
        group.setName(name);

        session.save(group);
        tx.commit();
    }

    public long getNumMembers() {
        return group.getNumMembers();
    }

    public OptIn optin(Group optinGroup) {
        List<OptIn> list = OptIn.findForGroup(group, SessionManager.session());
        return OptIn.findOptin(list, optinGroup);
    }

    @Override
    public boolean isLockedOutRecursive(Request request) {
        return false;
    }

    public Map<String, String> getSignupPages() {
        Map<String, String> map = new HashMap<>();
        for (GroupInWebsite giw : GroupInWebsite.findByGroup(group, SessionManager.session())) {
            Website w = giw.getWebsite();
            String domainName = w.getDomainName();
            if (domainName == null) {
                domainName = w.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
            }
            String url = "http://" + domainName + _(Formatter.class).getPortString() + "/" + group.getName() + "/signup";
            map.put(domainName, url);
        }
        return map;
    }

    public Map<String, String> getDataCaptureFields() {
        NvSet nvset = group.getFieldset();
        Map<String, String> map = new HashMap<>();
        if (nvset != null) {
            if (nvset.getNvPairs() != null) {
                for (NvPair nvp : nvset.getNvPairs()) {
                    map.put(nvp.getName(), nvp.getPropValue());
                }
            }
        }
        return map;
    }

    private void addField(String name, String value, Session session) {
        Date now = _(CurrentDateService.class).getNow();
        NvSet nvset = group.getFieldset();
        if (nvset == null) {
            nvset = new NvSet();
            nvset.setCreatedDate(now);
            nvset.setNvPairs(new HashSet<NvPair>());
        } else {
            nvset = nvset.duplicate(now, session);
        }
        session.save(nvset);
        NvPair newPair = nvset.addPair(name, value);
        session.save(newPair);
        group.setFieldset(nvset);
        session.save(group);
        log.info("addField. added: " + newPair.getId());
    }

    private void removeField(String name, Session session) {
        Date now = _(CurrentDateService.class).getNow();
        NvSet nvset = group.getFieldset();
        if (nvset == null || nvset.isEmpty()) {
            return;
        } else {
            nvset = nvset.duplicate(now, session);
        }
        nvset.removePair(name);
        session.save(nvset);
        group.setFieldset(nvset);
        session.save(group);

        log.info("removeField: removed " + name + " and saved: " + nvset.getId());
    }
}
