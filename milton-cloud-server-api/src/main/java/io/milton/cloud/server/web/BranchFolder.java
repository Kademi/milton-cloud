package io.milton.cloud.server.web;

import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Permission;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Commit;
import io.milton.vfs.data.HashCalc;
import io.milton.http.*;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import io.milton.vfs.content.ContentSession;
import io.milton.vfs.content.ContentSession.DirectoryNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Represents the current version of a branch in a repository
 *
 * This behaves much the same as a DirectoryResource but is defined differently
 *
 *
 * @author brad
 */
public class BranchFolder extends AbstractCollectionResource implements ContentDirectoryResource, PropFindableResource, MakeCollectionableResource, GetableResource, PutableResource, PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    private final boolean renderMode;
    private final CommonCollectionResource parent;
    private final String name;
    private final Branch branch;
    private ResourceList children;
    private Commit commit; // may be null
    private ContentSession contentSession;
    /**
     * if set html resources will be rendered with the templater
     */
    private String theme;
    private JsonResult jsonResult; // set after completing a POST

    public BranchFolder(String name, CommonCollectionResource parent, Branch branch, boolean renderMode) {
        super(parent.getServices());
        this.renderMode = renderMode;
        this.name = name;
        this.parent = parent;
        this.branch = branch;
        this.commit = branch.getHead();
        this.contentSession = new ContentSession(SessionManager.session(), branch, getServices().getCurrentDateService(), getServices().getHashStore(), getServices().getBlobStore());
    }

    @Override
    public void save() {
        System.out.println("BranchFolder: save session");
        UserResource currentUser = (UserResource) HttpManager.request().getAuthorization().getTag();
        contentSession.save(currentUser.getThisUser());
    }

    @Override
    public Resource child(String childName) {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() {
        if (children == null) {
            children = Utils.toResources(this, contentSession.getRootContentNode(), renderMode);
        }
        return children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        log.trace("createCollection: " + newName);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryResource rdr = createDirectoryResource(newName, session);
        tx.commit();
        return rdr;
    }

    public DirectoryResource createDirectoryResource(String newName, Session session) throws NotAuthorizedException, ConflictException, BadRequestException {
        ContentSession.DirectoryNode newNode = contentSession.getRootContentNode().addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this, services, false);
        onAddedChild(rdr);
        save();
        return rdr;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryNode thisNode = contentSession.getRootContentNode();
        ContentSession.FileNode newFileNode = thisNode.addFile(newName);
        FileResource fileResource = new FileResource(newFileNode, this, services);

        String ct = HttpManager.request().getContentTypeHeader();
        if (ct != null && ct.equals("spliffy/hash")) {
            // read the new hash and set it on this
            DataInputStream din = new DataInputStream(inputStream);
            long newHash = din.readLong();
            newFileNode.setHash(newHash);
        } else {
            log.info("createNew: set content");
            // parse data and persist to stores
            newFileNode.setContent(inputStream);
        }
        onAddedChild(fileResource);
        save();
        tx.commit();

        return fileResource;

    }

    @Override
    public Date getCreateDate() {
        return contentSession.getRootContentNode().getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return contentSession.getRootContentNode().getModifedDate();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
            return;
        }
        String type = params.get("type");
        if (type == null) {
            // output directory listing
            log.trace("sendContent: render template");
            getTemplater().writePage("repoHome", this, params, out);
        } else {
            log.trace("sendContent: " + type);
            switch (type) {
                case "hashes":
                    HashCalc.getInstance().calcHash(contentSession.getRootContentNode().getDataNode(), out);
                    break;
                case "hash":
                    String s = contentSession.getRootContentNode().getDataNode().getHash() + "";
                    out.write(s.getBytes());                    
                    break;
            }
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        if (jsonResult != null) {
            return "application/x-javascript; charset=utf-8";
        }
        String type = HttpManager.request().getParams().get("type");
        if (type == null || type.length() == 0) {
            return "text/html";
        } else {
            if (type.equals("hashes") || type.equals("revision")) {
                return "text/plain";
            } else {
                return type;
            }
        }
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    /**
     * may be null
     *
     * @return
     */
    public Commit getRepoVersion() {
        return commit;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        Set<Permission> perms = SecurityUtils.getPermissions(user, branch, SessionManager.session());
        SecurityUtils.addPermissions(perms, list);
        parent.addPrivs(list, user);
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
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
//        String shareWith = parameters.get("shareWith");
//        String priv = parameters.get("priviledge");
//        AccessControlledResource.Priviledge p = AccessControlledResource.Priviledge.valueOf(priv);
//        String message = parameters.get("message");
//        if (shareWith != null) {
//            getServices().getShareManager().sendShareInvites(getCurrentUser(), repository, shareWith, p, message);
//            this.jsonResult = new JsonResult(true);
//        }
        return null;

    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    public String getTitle() {
        return this.branch.getRepository().getTitle();
    }

    @Override
    public DirectoryNode getDirectoryNode() {
        return contentSession.getRootContentNode();
    }

    @Override
    public void onAddedChild(AbstractContentResource aThis) {
        getChildren().add(aThis);
    }

    @Override
    public void onRemovedChild(AbstractContentResource aThis) {
        getChildren().remove(aThis);
    }
}
