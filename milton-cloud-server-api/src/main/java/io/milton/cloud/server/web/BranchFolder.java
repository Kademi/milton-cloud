package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
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
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.LockMode;

/**
 * Represents the current version of a branch in a repository
 *
 * This behaves much the same as a DirectoryResource but is defined differently
 *
 *
 * @author brad
 */
public class BranchFolder extends AbstractCollectionResource implements ContentDirectoryResource, PropFindableResource, MakeCollectionableResource, GetableResource, PutableResource, PostableResource, NodeChildUtils.ResourceCreator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    protected final boolean renderMode;
    protected final CommonCollectionResource parent;
    protected final String name;
    protected final Branch branch;
    protected ResourceList children;
    protected Commit commit; // may be null
    protected final DataSession dataSession;
    /**
     * if set html resources will be rendered with the templater
     */
    private String theme;
    private JsonResult jsonResult; // set after completing a POST
    protected NodeMeta nodeMeta;
    private RenderFileResource indexPage;

    public BranchFolder(String name, CommonCollectionResource parent, Branch branch, boolean renderMode) {
        this.renderMode = renderMode;
        this.name = name;
        this.parent = parent;
        this.branch = branch;
        this.commit = branch.getHead();
        this.dataSession = new DataSession(branch, SessionManager.session(), _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
    }

    @Override
    public String getUniqueId() {
        return "br" + branch.getId();
    }

    @Override
    public void save() {
        UserResource currentUser = (UserResource) HttpManager.request().getAuthorization().getTag();
        dataSession.save(currentUser.getThisUser());
    }

    @Override
    public Resource child(String childName) {
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() {
        if (children == null) {
            System.out.println("BranchFolder: getChildren - " + getName());
            children = NodeChildUtils.toResources(this, dataSession.getRootDataNode(), renderMode, this);
        }
        return children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        log.info("createCollection: " + newName);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryResource rdr = createDirectoryResource(newName, session);
        tx.commit();
        return rdr;
    }

    public DirectoryResource createDirectoryResource(String newName, Session session) throws NotAuthorizedException, ConflictException, BadRequestException {
        DirectoryNode newNode = dataSession.getRootDataNode().addDirectory(newName);
        DirectoryResource rdr = newDirectoryResource(newNode, this, false);
        onAddedChild(rdr);
        rdr.updateModDate();
        save();
        return rdr;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryNode thisNode = dataSession.getRootDataNode();
        FileNode newFileNode = thisNode.addFile(newName);
        FileResource fileResource = newFileResource(newFileNode, this, false);

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
        updateModDate();
        save();
        tx.commit();

        return fileResource;
    }

    protected void updateModDate() {
        Date newDate = _(CurrentDateService.class).getNow();
        loadNodeMeta().setModDate(newDate);
        if (nodeMeta.getCreatedDate() == null) {
            nodeMeta.setCreatedDate(newDate);
        }
        try {
            NodeMeta.saveMeta(this.getDirectoryNode(), nodeMeta);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Date getCreateDate() {
        return loadNodeMeta().getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return loadNodeMeta().getModDate();
    }

    private NodeMeta loadNodeMeta() {
        if (nodeMeta == null) {
            try {
                nodeMeta = NodeMeta.loadForNode(getDirectoryNode());
            } catch (IOException ex) {
                log.error("Couldnt load meta", ex);
                nodeMeta = new NodeMeta(null, null, null, 0);
            }
        }
        return nodeMeta;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
            return;
        }
        String type = params.get("type");
        if (type == null) {
            renderPage(out, params);

        } else {
            log.trace("sendContent: " + type);
            switch (type) {
                case "hashes":
                    HashCalc.getInstance().calcHash(dataSession.getRootDataNode(), out);
                    break;
                case "hash":
                    String s = dataSession.getRootDataNode().getHash() + "";
                    out.write(s.getBytes());
                    break;
            }
        }
    }

    protected void renderPage(OutputStream out, Map<String, String> params) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (renderMode) {
            if (getIndex() != null) {
                getIndex().sendContent(out, null, params, null);
                return;
            }
        }
        log.trace("sendContent: render template");
        getTemplater().writePage(false, "repoHome", this, params, out);
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
        return dataSession.getRootDataNode();
    }

    @Override
    public void onAddedChild(AbstractContentResource aThis) {
        getChildren().add(aThis);
    }

    @Override
    public void onRemovedChild(AbstractContentResource aThis) {
        getChildren().remove(aThis);
    }

    @Override
    public FileResource newFileResource(FileNode dm, ContentDirectoryResource parent, boolean renderMode) {
        return new FileResource(dm, parent);
    }

    @Override
    public DirectoryResource newDirectoryResource(DirectoryNode dm, ContentDirectoryResource parent, boolean renderMode) {
        return new DirectoryResource(dm, parent, renderMode);
    }

    @Override
    public boolean is(String type) {
        if ("branch".equals(type) || "folder".equals(type) || "directory".equals(type)) {
            return true;
        }
        return super.is(type);
    }

    public RenderFileResource getIndex() throws NotAuthorizedException, BadRequestException {
        if (indexPage == null) {
            Resource r = child("index.html");
            if (r == null) {
                return null;
            } else if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                indexPage = fr.getHtml();
            } else if (r instanceof RenderFileResource) {
                indexPage = (RenderFileResource) r;
            } else {
                return null;
            }
        }
        return indexPage;
    }

    @Override
    public boolean isPublic() {
        return parent.isPublic();
    }

    public Branch getBranch() {
        return branch;
    }
}
