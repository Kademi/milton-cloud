package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.Version;
import io.milton.cloud.server.manager.CommentService;
import io.milton.cloud.server.web.templating.TitledPage;
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
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.annotations.BeanProperty;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Repository;

/**
 *
 * @author brad
 */
public abstract class AbstractContentResource<T extends DataNode, P extends ContentDirectoryResource> extends AbstractResource implements ContentResource, PropFindableResource, GetableResource, DeletableResource, CopyableResource, MoveableResource, TitledPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractContentResource.class);
    protected P parent;
    protected T contentNode;
    protected NodeMeta nodeMeta;

    /**
     *
     * @param contentNode - the current item version for this resource
     * @param parent - Primary parent, ie that which located the resource in
     * this request. May be null when looking for linked resources
     * @param parents - All parents. May be null in cases where the resource is
     * freshly created, in which case the given parent is the set
     * @param services
     */
    public AbstractContentResource(T contentNode, P parent) {
        this.contentNode = contentNode;
        this.parent = parent;
    }

    public AbstractContentResource(P parent) {
        this.parent = parent;
    }

    @Override
    public void moveTo(CollectionResource rDest, String newName) throws ConflictException, NotAuthorizedException, BadRequestException {
        if (!(rDest instanceof ContentDirectoryResource)) {
            throw new ConflictException(this, "Can't move to: " + rDest.getClass());
        }
        log.info("moveTo: " + rDest.getName() + " " + newName);
        log.info("old location: " + parent.getName() + "/" + getName());
        ContentDirectoryResource newParent = (ContentDirectoryResource) rDest;
        ContentDirectoryResource oldParent = parent;

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        DirectoryNode newParentNode = newParent.getDirectoryNode();
        contentNode.move(newParentNode, newName);
        parent = (P) newParent;
        newParent.onAddedChild(this);
        oldParent.onRemovedChild(this);
        try {
            newParent.save();
        } catch (IOException ex) {
            throw new BadRequestException("io ex", ex);
        }
        tx.commit();
    }

    @Override
    public void copyTo(CollectionResource toCollection, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
        log.info("copyTo: " + toCollection.getName() + " newName=" + newName);
        if (toCollection instanceof ContentDirectoryResource) {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();

            ContentDirectoryResource newParent = (ContentDirectoryResource) toCollection;
            DirectoryNode newDir = newParent.getDirectoryNode();
            contentNode.copy(newDir, newName);
            try {
                newParent.save();
            } catch (IOException ex) {
                throw new BadRequestException("io ex", ex);
            }
            tx.commit();
        } else {
            throw new ConflictException(this, "Can't copy to collection of type: " + toCollection.getClass());
        }
    }
    

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        doDelete();
        try {
            parent.save();
        } catch (IOException ex) {
            throw new BadRequestException("io ex", ex);
        }
        tx.commit();
    }

    /**
     * Deletes the content node and removes from parent, but does not do a save
     * or commit
     */
    public void doDelete() {
        if( contentNode != null ) { // allowed to be null for subclasses which have optional content node
            contentNode.delete();
        }
        parent.onRemovedChild(this);

    }

    @Override
    public Date getCreateDate() {
        if (this.contentNode == null) {
            return null;
        }
        return loadNodeMeta().getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        if (this.contentNode == null) {
            return null;
        }
        return loadNodeMeta().getModDate();
    }

    public void updateModDate() {
        long previousProfileId = loadNodeMeta().getProfileId();
        Date previousModDate = loadNodeMeta().getModDate();
        String previousResourceHash = contentNode.getLoadedHash();

        String newResourceHash = contentNode.getHash();
        Date newDate = _(CurrentDateService.class).getNow();
        long newProfileId = 0;
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        if (p != null) {
            newProfileId = p.getId();
        }

        Version.insert(previousResourceHash, previousModDate, previousProfileId, newResourceHash, newDate, newProfileId, SessionManager.session());

        nodeMeta.setModDate(newDate);
        nodeMeta.setProfileId(newProfileId);
        if (nodeMeta.getCreatedDate() == null) {
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
        if (contentNode != null) {
            NodeMeta meta = loadNodeMeta();
            if (meta != null) {
                UUID id = meta.getId();
                if (id != null) {
                    return id.toString();
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        if (contentNode != null) {
            return contentNode.getName();
        } else {
            return null;
        }
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
    public P getParent() {
        return parent;
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
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @BeanProperty
    public List<CommentBean> getComments() {
        return _(CommentService.class).comments(this);
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
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
            Session session = SessionManager.session();
            Transaction tx = SessionManager.beginTx();
            _(CommentService.class).newComment(this, s, wrf.getWebsite(), currentUser, session);
            tx.commit();
        }
    }

    /**
     * This is just here to make newComment a bean property
     *
     * @return
     */
    @BeanProperty( writeRole = Priviledge.READ)
    public String getNewComment() {
        return null;
    } 

    @Override
    public boolean isPublic() {
        if( parent.getBranch() != null ) {
            Branch b = parent.getBranch();
            if( b.getRepository() != null ) {
                return b.getRepository().isPublicContent();
            }
        }
        return false;
    }

    /**
     * For public repositories we allow all READ operations
     *
     * TODO: should limit this to not include PROPFIND TODO: a POST is often
     * available to anonymous users but will be rejected
     *
     * @param request
     * @param method
     * @param auth
     * @return
     */
    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        if (!method.isWrite) {
            if (isPublic()) {
                return true;
            }
        }
        return super.authorise(request, method, auth);
    }

    @Override
    public String getHash() {
        return this.contentNode.getHash();
    }

    @Override
    public Profile getModifiedBy() {
        NodeMeta meta = loadNodeMeta();
        if (meta == null) {
            return null;
        }
        return Profile.get(meta.getProfileId(), SessionManager.session());
    }

    @Override
    public void save() throws IOException {
        getParent().save();
    }

    @Override
    public void setHash(String s) {
        this.contentNode.setHash(s);
        updateModDate();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }

    @Override
    public Branch getBranch() {
        if (parent == null) {
            log.warn("No branch, because no parent: " + getClass());
            return null;
        }
        return parent.getBranch();
    }

    @Override
    public Repository getRepository() {
        Branch b = getBranch();
        if (b != null) {
            return b.getRepository();
        }
        return null;
    }

    @Override
    public Profile getOwnerProfile() {
        Branch b = getBranch();
        if (b == null) {
            log.warn("Null branch for this content resource: " + getClass());
            return null;
        }
        BaseEntity be = b.getRepository().getBaseEntity();
        if (be instanceof Profile) {
            return (Profile) be;
        }
        return null;
    }
}
