package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.manager.CommentService;
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
import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public abstract class AbstractContentResource extends AbstractResource implements CommonResource, PropFindableResource, GetableResource, DeletableResource, CopyableResource, MoveableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractContentResource.class);
    protected ContentDirectoryResource parent;
    protected DataNode contentNode;
    protected NodeMeta nodeMeta;
    
    public abstract String getTitle();

    /**
     *
     * @param contentNode - the current item version for this resource
     * @param parent - Primary parent, ie that which located the resource in
     * this request. May be null when looking for linked resources
     * @param parents - All parents. May be null in cases where the resource is
     * freshly created, in which case the given parent is the set
     * @param services
     */
    public AbstractContentResource(DataNode contentNode, ContentDirectoryResource parent) {
        this.contentNode = contentNode;
        this.parent = parent;
    }

    public AbstractContentResource(ContentDirectoryResource parent) {
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

        DirectoryNode newParentNode = newParent.getDirectoryNode();
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
            DirectoryNode newDir = newParent.getDirectoryNode().addDirectory(newName);
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
        return loadNodeMeta().getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return loadNodeMeta().getModDate();
    }

    protected void updateModDate() {
        Date newDate = _(CurrentDateService.class).getNow();
        loadNodeMeta().setModDate(newDate);
        if( nodeMeta.getCreatedDate() == null ) {
            nodeMeta.setCreatedDate(newDate);
        }
        try {
            NodeMeta.saveMeta(contentNode, nodeMeta);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public NodeMeta loadNodeMeta() {
        if (nodeMeta == null) {
            try {
                nodeMeta = NodeMeta.loadForNode(contentNode);
            } catch (IOException ex) {
                throw new RuntimeException("Couldnt load meta");
                //log.error("Couldnt load meta", ex);
                //nodeMeta = new NodeMeta(null, null, null);
            }
        }
        return nodeMeta;
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
        while (!(col instanceof BranchFolder)) {
            col = col.getParent();
        }
        BranchFolder rr = (BranchFolder) col;
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
    

    public List<CommentBean> getComments() {
        return _(CommentService.class).comments(this.loadNodeMeta().getId()); 
    }

    public int getNumComments() {
        List<CommentBean> list = getComments();
        if (list == null) {
            return 0;
        } else {
            return list.size();
        }
    }

    public void setNewComment(String s) throws NotAuthorizedException {
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        if( rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
            _(CommentService.class).newComment(this, s, wrf.getWebsite(), currentUser, SessionManager.session());
        }
    }

    /**
     * This is just here to make newComment a bean property
     *
     * @return
     */
    public String getNewComment() {
        return null;
    }

    @Override
    public boolean isPublic() {
        return parent.isPublic();
    }

    
}
