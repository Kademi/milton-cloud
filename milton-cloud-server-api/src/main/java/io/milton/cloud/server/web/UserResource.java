package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.hibernate.Transaction;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.manager.PasswordManager;
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
import io.milton.mail.Mailbox;
import io.milton.mail.MessageFolder;
import io.milton.resource.*;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import javax.mail.internet.MimeMessage;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.principal.CalDavPrincipal;
import io.milton.principal.CardDavPrincipal;

/**
 *
 * @author brad
 */
public class UserResource extends AbstractCollectionResource implements CollectionResource, MakeCollectionableResource, PropFindableResource, GetableResource, PrincipalResource, Mailbox, CalDavPrincipal, CardDavPrincipal {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserResource.class);
    private final Profile user;
    private final CommonCollectionResource parent;
    private ResourceList children;

    public UserResource(CommonCollectionResource parent, Profile u) {
        this.parent = parent;
        this.user = u;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        if( auth != null && auth.getTag() != null ) {
            UserResource ur = (UserResource) auth.getTag();
            if( ur.getThisUser() == user ) {
                return true;
            }
        }
        return super.authorise(request, method, auth);
    }

    
    
    public String getTitle() {
        String s = user.getNickName();
        if( s == null ) {
            s = user.getFirstName();
        }
        if( s == null ) {
            s = user.getName();
        }
        return s;
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


    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (user.getRepositories() != null) {
                for (Repository r : user.getRepositories()) {
                    Branch b = r.getTrunk();
                    if( b != null ) {
                        BranchFolder rr = new BranchFolder(r.getName(), this, b, false);
                        children.add(rr);
                    }
                }
            }
            _(ApplicationManager.class).addBrowseablePages(this, children);
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
        Repository r = user.createRepository(newName, user, SessionManager.session());
        Branch b = r.getTrunk();
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
        getTemplater().writePage("user/dashboard", this, params, out);
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
        //addPrivs(list, user);
        map.put(this, list);
        return map;
    }

//    @Override
//    public List<LdapContact> searchContacts(Condition condition, int maxCount) {
//        log.info("searchContacts: " + condition);
//        SessionManager.session().lock(user, LockMode.NONE);
//        try {
//            List<LdapContact> results = new ArrayList<>();
//
//            for (Resource r : getChildren()) {
//                if (r instanceof ContactsHomeFolder) {
//                    ContactsHomeFolder contactsHomeFolder = (ContactsHomeFolder) r;
//                    for (Resource r2 : contactsHomeFolder.getChildren()) {
//                        if (r2 instanceof ContactsFolder) {
//                            ContactsFolder cf = (ContactsFolder) r2;
//                            for (Resource r3 : cf.getChildren()) {
//                                if (r3 instanceof LdapContact) {
//                                    LdapContact ldapContact = (LdapContact) r3;
//                                    if (condition == null || condition.isMatch(ldapContact)) {
//                                        log.trace("searchContacts: contact matches search criteria: " + ldapContact.getName());
//                                        results.add(ldapContact);
//                                    }
//                                }
//
//                            }
//                        }
//                    }
//                }
//            }
//
//            log.trace("searchContacts: " + getName() + ", results ->" + results.size());
//            return results;
//        } catch (NotAuthorizedException | BadRequestException ex) {
//            throw new RuntimeException(ex);
//        }
//    }

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

    @Override
    public boolean authenticate(String password) {
        return _(PasswordManager.class).verifyPassword(user, password);
    }

    @Override
    public boolean authenticateMD5(byte[] passwordHash) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MessageFolder getInbox() {
        try {
            Resource dir = child("inbox");            
            if( dir instanceof MessageFolder) {
                MessageFolder emailFolder = (MessageFolder) dir;
                return emailFolder;
            } else {
                throw new RuntimeException("inbox folder is not a valid mesasge folder");
            }
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
        
    }

    @Override
    public MessageFolder getMailFolder(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmailDisabled() {
        return false;
    }

    @Override
    public void storeMail(MimeMessage mm) {
        _(ApplicationManager.class).storeMail(this, mm);
    }
    
    public String getNickName() {
        String s = user.getNickName();
        if( s == null || s.length() == 0 ) {
            s = user.getFirstName();
        }
        if( s == null ) {
            s = user.getName();
        }
        return s;
    }
    
    public Long getUserId() {
        return user.getId();
    }
}
