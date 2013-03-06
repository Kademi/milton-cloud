package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.resource.AccessControlledResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.HrefPrincipleId;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.mail.Mailbox;
import io.milton.mail.MessageFolder;
import io.milton.resource.*;
import javax.mail.internet.MimeMessage;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.*;

/**
 *
 * @author brad
 */
public class GroupResource extends AbstractCollectionResource implements CollectionResource, PropFindableResource, GetableResource, PrincipalResource, Mailbox, IGroupResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupResource.class);
    private final Group group;
    private final CommonCollectionResource parent;
    private ResourceList children;

    public GroupResource(CommonCollectionResource parent, Group u) {
        this.parent = parent;
        this.group = u;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public Date getCreateDate() {
        return group.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return group.getModifiedDate();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        // TODO: should connect template selection to the Application which produced this resource
        getTemplater().writePage("user/profile", this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
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
        return map;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean authenticate(String password) {
        return false;
    }

    @Override
    public boolean authenticateMD5(byte[] passwordHash) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MessageFolder getInbox() {
        return _(ApplicationManager.class).getInbox(this);
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

    @Override
    public Group getGroup() {
        return group;
    }
    
    
}
