package io.milton.cloud.server.web;


import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Profile;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.ItemHistory;
import io.milton.vfs.db.MetaItem;
import io.milton.cloud.server.db.*;
import io.milton.http.AccessControlledResource;
import io.milton.http.Auth;
import io.milton.http.acl.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import io.milton.vfs.content.ContentSession.ContentNode;
import io.milton.vfs.db.utils.SessionManager;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public abstract class AbstractMutableResource extends AbstractResource implements PropFindableResource, GetableResource, DeletableResource, MutableResource, CopyableResource, MoveableResource {

    protected final MutableCollection parent;
    protected ContentNode contentNode;    
        
    /**
     * 
     * @param contentNode - the current item version for this resource
     * @param parent - Primary parent, ie that which located the resource in this request. May be null when looking for linked resources
     * @param parents - All parents. May be null in cases where the resource is freshly created, in which case the given parent is the set
     * @param services 
     */
    public AbstractMutableResource( ContentNode contentNode, MutableCollection parent, Services services) {
        super(services);
        this.contentNode = contentNode;
        this.parent = parent;

    }

    
    @Override
    public void moveTo(CollectionResource rDest, String newName) throws ConflictException, NotAuthorizedException, BadRequestException {

        if (!(rDest instanceof MutableCollection)) {
            throw new ConflictException(this, "Can't move to: " + rDest.getClass());
        }
        MutableCollection newParent = (MutableCollection) rDest;

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (newParent.getItemVersion().getId() == parent.getItemVersion().getId()) {
            // just rename
            this.name = newName;
            parent.onChildChanged(this);
            parent.save(session);
        } else {
            parent.removeChild(this);
            newParent.addChild(this);
            newParent.save(session); // save calls up to RepositoryFolder which will call back down to save dirty nodes, including old parent
        }

        tx.commit();
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        DeletedItem deletedItem = new DeletedItem();
        if (parent.getItemVersion() != null) { // will be null for folders directly in a Commit
            deletedItem.setDeletedFrom(parent.getItemVersion());

        }
        deletedItem.setDeletedResource(getItemVersion());
        deletedItem.setRepoVersion(currentRepoVersion());
        session.save(deletedItem);

        parent.removeChild(this);
        parent.save(session);
        tx.commit();
    }

    @Override
    public Date getCreateDate() {
        return itemVersion.getItem().getCreateDate();
    }

    @Override
    public Date getModifiedDate() {
        return itemVersion.getModifiedDate();
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
    public Long getEntryHash() {
        return hash;
    }

    public void setHash(long hash) {
        if (this.hash != hash) {
            //dirty = true;
        }
        this.hash = hash;
    }

    @Override
    public MetaItem getItemVersion() {
        return itemVersion;
    }

    @Override
    public void setItemVersion(MetaItem itemVersion) {
        this.itemVersion = itemVersion;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    public Commit currentRepoVersion() {
        SpliffyResource col = parent;
        while (!(col instanceof RepositoryFolder)) {
            col = col.getParent();
        }
        RepositoryFolder rr = (RepositoryFolder) col;
        return rr.getRepoVersion();
    }

    @Override
    public MutableCollection getParent() {
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
    public ItemHistory getDirectoryMember() {
        return dm;
    }
    
    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
    
    
}
