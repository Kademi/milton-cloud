package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Commit;
import io.milton.resource.AccessControlledResource;
import io.milton.http.Auth;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import io.milton.vfs.content.ContentSession;
import io.milton.vfs.content.ContentSession.ContentNode;
import io.milton.vfs.db.utils.SessionManager;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public abstract class AbstractContentResource extends AbstractResource implements CommonResource, PropFindableResource, GetableResource, DeletableResource, CopyableResource, MoveableResource {

    protected ContentDirectoryResource parent;
    protected ContentNode contentNode;

    /**
     *
     * @param contentNode - the current item version for this resource
     * @param parent - Primary parent, ie that which located the resource in
     * this request. May be null when looking for linked resources
     * @param parents - All parents. May be null in cases where the resource is
     * freshly created, in which case the given parent is the set
     * @param services
     */
    public AbstractContentResource(ContentNode contentNode, ContentDirectoryResource parent, Services services) {
        super(services);
        this.contentNode = contentNode;
        this.parent = parent;

    }

    @Override
    public void moveTo(CollectionResource rDest, String newName) throws ConflictException, NotAuthorizedException, BadRequestException {

        if (!(rDest instanceof ContentDirectoryResource)) {
            throw new ConflictException(this, "Can't move to: " + rDest.getClass());
        }
        ContentDirectoryResource newParent = (ContentDirectoryResource) rDest;
        ContentDirectoryResource oldParent = parent;

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        ContentSession.DirectoryNode newParentNode = newParent.getDirectoryNode();
        contentNode.move(newParentNode, newName);
        parent = newParent;
        newParent.onAddedChild(this);
        oldParent.onRemovedChild(this);
        newParent.save();
        tx.commit();
    }

    @Override
    public void copyTo(CollectionResource toCollection, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
        if (toCollection instanceof ContentDirectoryResource) {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();

            ContentDirectoryResource newParent = (ContentDirectoryResource) toCollection;
            ContentSession.DirectoryNode newDir = newParent.getDirectoryNode().addDirectory(newName);
            contentNode.copy(newDir, newName);
            parent.save();
            tx.commit();
        } else {
            throw new ConflictException(this, "Can't copy to collection of type: " + toCollection.getClass());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        contentNode.delete();
        parent.onRemovedChild(this);
        parent.save();
        tx.commit();
    }

    @Override
    public Date getCreateDate() {
        return contentNode.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return contentNode.getModifedDate();
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return contentNode.getName();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    public Commit currentRepoVersion() {
        CommonResource col = parent;
        while (!(col instanceof RepositoryFolder)) {
            col = col.getParent();
        }
        RepositoryFolder rr = (RepositoryFolder) col;
        return rr.getRepoVersion();
    }

    @Override
    public ContentDirectoryResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner(); // go up until we get an entity
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
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        // TODO: if this is a linked folder this won't be right!!!
        parent.addPrivs(list, user);
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
