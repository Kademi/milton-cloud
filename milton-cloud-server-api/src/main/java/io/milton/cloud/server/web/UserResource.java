package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.hibernate.LockMode;
import org.hibernate.Transaction;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.calendar.CalendarFolder;
import io.milton.cloud.server.apps.calendar.CalendarHomeFolder;
import io.milton.cloud.server.apps.contacts.ContactsFolder;
import io.milton.cloud.server.apps.contacts.ContactsHomeFolder;
import io.milton.resource.AccessControlledResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.HrefPrincipleId;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.HrefList;
import io.milton.ldap.Condition;
import io.milton.resource.*;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class UserResource extends AbstractCollectionResource implements CollectionResource, MakeCollectionableResource, PropFindableResource, GetableResource, PrincipalResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserResource.class);
    private final Profile user;
    private final CommonCollectionResource parent;
    private final ApplicationManager applicationManager;
    private ResourceList children;

    public UserResource(CommonCollectionResource parent, Profile u, ApplicationManager applicationManager) {
        super(parent.getServices());
        this.parent = parent;
        this.user = u;
        this.applicationManager = applicationManager;
    }

    public List<BranchFolder> getRepositories() throws NotAuthorizedException, BadRequestException {
        List<BranchFolder> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if (r instanceof BranchFolder) {
                list.add((BranchFolder) r);
            }
        }
        return list;
    }

    public List<CalendarFolder> getCalendars() throws NotAuthorizedException, BadRequestException {
        List<CalendarFolder> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if (r instanceof CalendarHomeFolder) {
                CalendarHomeFolder calHome = (CalendarHomeFolder) r;
                for (Resource r2 : calHome.getChildren()) { 
                    if (r2 instanceof CalendarFolder) {
                        list.add((CalendarFolder) r2);
                    }
                }
            }
        }
        return list;
    }

    public List<ContactsFolder> getAddressBooks() throws NotAuthorizedException, BadRequestException {
        List<ContactsFolder> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if (r instanceof ContactsHomeFolder) {
                ContactsHomeFolder home = (ContactsHomeFolder) r;
                for (Resource r2 : home.getChildren()) {
                    if (r2 instanceof ContactsFolder) {
                        list.add((ContactsFolder) r2);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (user.getRepositories() != null) {
                for (Repository r : user.getRepositories()) {
                    Branch b = r.trunk(SessionManager.session());
                    BranchFolder rr = new BranchFolder(r.getName(), this, b, false);
                    children.add(rr);
                }
            }
            applicationManager.addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Transaction tx = SessionManager.session().beginTransaction();
        Repository r = new Repository();
        r.setBaseEntity(user);
        r.setName(newName);
        r.setCreatedDate(new Date());
        List<Repository> list = user.getRepositories();
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(r);
        Branch b = r.trunk(SessionManager.session());

        SessionManager.session().save(r);
        tx.commit();
        return new BranchFolder(r.getName(), this, b, false);
    }

    @Override
    public Date getCreateDate() {
        return user.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return user.getModifiedDate();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        // TODO: should connect template selection to the Application which produced this resource
        getTemplater().writePage("userHome", this, params, out);
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
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return user;
    }

    @Override
    public PrincipleId getIdenitifer() {
        return new HrefPrincipleId(getHref());
    }

    @Override
    public HrefList getCalendarHomeSet() {
        return HrefList.asList(getHref() + "calendars/");
    }

    @Override
    public HrefList getCalendarUserAddressSet() {
        return HrefList.asList(getHref());
    }

    @Override
    public String getScheduleInboxUrl() {
        return null;
    }

    @Override
    public String getScheduleOutboxUrl() {
        return null;
    }

    @Override
    public String getDropBoxUrl() {
        return null;
    }

    @Override
    public HrefList getAddressBookHomeSet() {
        return HrefList.asList(getHref() + "abs/"); // the address books folder
    }

    @Override
    public String getAddress() {
        return getHref() + "abs/";
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile u) {
        // Give this user special permissions
        if (u != null) {
            if (user.getName().equals(u.getName())) {
                list.add(Priviledge.READ);
                list.add(Priviledge.WRITE);
                list.add(Priviledge.READ_ACL);
                list.add(Priviledge.UNLOCK);
                list.add(Priviledge.WRITE_CONTENT);
                list.add(Priviledge.WRITE_PROPERTIES);
            }
        }
        parent.addPrivs(list, u);
    }

    /**
     * Get all allowed priviledges for all principals on this resource. Note
     * that a principal might be a user, a group, or a built-in webdav group
     * such as AUTHENTICATED
     *
     * @return
     */
    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        Map<Principal, List<AccessControlledResource.Priviledge>> map = new HashMap<>();
        List<Priviledge> list = new ArrayList<>();
        addPrivs(list, user);
        map.put(this, list);
        return map;
    }

    @Override
    public List<LdapContact> searchContacts(Condition condition, int maxCount) {
        log.info("searchContacts: " + condition);
        SessionManager.session().lock(user, LockMode.NONE);
        try {
            List<LdapContact> results = new ArrayList<>();

            for (Resource r : getChildren()) {
                if (r instanceof ContactsHomeFolder) {
                    ContactsHomeFolder contactsHomeFolder = (ContactsHomeFolder) r;
                    for (Resource r2 : contactsHomeFolder.getChildren()) {
                        if (r2 instanceof ContactsFolder) {
                            ContactsFolder cf = (ContactsFolder) r2;
                            for (Resource r3 : cf.getChildren()) {
                                if (r3 instanceof LdapContact) {
                                    LdapContact ldapContact = (LdapContact) r3;
                                    if (condition == null || condition.isMatch(ldapContact)) {
                                        log.trace("searchContacts: contact matches search criteria: " + ldapContact.getName());
                                        results.add(ldapContact);
                                    }
                                }

                            }
                        }
                    }
                }
            }

            log.trace("searchContacts: " + getName() + ", results ->" + results.size());
            return results;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the user this resource represents
     * 
     * @return 
     */
    public Profile getThisUser() {
        return user;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
