/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.ApplicationManager;
import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public abstract class AbstractBranchFolder extends AbstractCollectionResource implements ContentDirectoryResource, PropFindableResource, MakeCollectionableResource, PutableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    protected final CommonCollectionResource parent;
    protected final String name;
    protected ResourceList children;
    private GetableResource indexPage;

    public abstract Branch getBranch(boolean createIfNeeded);

    public abstract DataSession getDataSession(boolean createIfNeeded);

    public abstract NodeMeta getNodeMeta();

    public AbstractBranchFolder(String name, CommonCollectionResource parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String getUniqueId() {
        Branch b = getBranch();
        if (b != null) {
            return "br" + b.getId();
        }
        return null;
    }

    @Override
    public void save() throws IOException {
        Branch branch = getBranch(true);
        if (branch == null) {
            throw new RuntimeException("Cannot save because the branch is null. this is probably because you're viewing a commit");
        }
        String lastHash = null;
        Long lastId = null;
        if (branch.getHead() != null) {
            lastId = branch.getHead().getId();
            lastHash = branch.getHead().getItemHash();
        }

        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("No current user!");
        }
        DataSession dataSession = getDataSession(true);
        dataSession.save(currentUser);

        System.out.println("----------------------------------");
        System.out.println("branch head ID: " + branch.getHead().getId());
        System.out.println("branch head hash: " + branch.getHead().getItemHash());
        System.out.println("last hash: " + lastHash);
        System.out.println("Last head id: " + lastId);
        System.out.println("----------------------------------");
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = _(ApplicationManager.class).getPage(this, childName);
        if (r != null) {
            return r;
        }

        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            System.out.println("getChildren: " + getName());
            ApplicationManager am = _(ApplicationManager.class);
            DataSession dataSession = getDataSession(false);
            if (dataSession != null) {
                children = am.toResources(this, dataSession.getRootDataNode());
            } else {
                children = new ResourceList();
            }
            am.addBrowseablePages(this, children);
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
        DirectoryResource rdr;
        try {
            rdr = createDirectoryResource(newName, session);
        } catch (IOException ex) {
            throw new BadRequestException("ioex", ex);
        }
        tx.commit();
        return rdr;
    }

    public DirectoryResource createDirectoryResource(String newName, Session session) throws NotAuthorizedException, ConflictException, BadRequestException, IOException {
        DataSession dataSession = getDataSession(true);
        DataSession.DirectoryNode newNode = dataSession.getRootDataNode().addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this);
        onAddedChild(rdr);
        rdr.updateModDate();
        save();
        return rdr;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        return UploadUtils.createNew(this, newName, inputStream, length, contentType);
    }

    protected void updateModDate() {
        Date newDate = _(CurrentDateService.class).getNow();
        NodeMeta nodeMeta = getNodeMeta();
        nodeMeta.setModDate(newDate);
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
        Branch b = getBranch();
        if( b == null ) {
            return null;
        }
        return b.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        Branch branch = getBranch(false);
        if (branch != null) {
            if (branch.getHead() != null) {
                return branch.getHead().getCreatedDate();
            } else {
                return branch.getCreatedDate();
            }
        } else {
            return null;
        }
    }

    @Override
    public CommonCollectionResource getParent() {
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

    @Override
    public DataSession.DirectoryNode getDirectoryNode() {
        return getDirectoryNode(false);
    }

    public DataSession.DirectoryNode getDirectoryNode(boolean autoCreate) {
        DataSession ds = getDataSession(autoCreate);
        if (ds != null) {
            return ds.getRootDataNode();
        } else {
            return null;
        }

    }

    @Override
    public void onAddedChild(AbstractContentResource aThis) {
        try {
            getChildren().add(aThis);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onRemovedChild(AbstractContentResource aThis) {
        try {
            getChildren().remove(aThis);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean is(String type) {
        if ("branch".equals(type)) {
            return true;
        }
        return super.is(type);
    }

    public GetableResource getIndex() throws NotAuthorizedException, BadRequestException {
        if (indexPage == null) {
            Resource r = child("index.html");
            if (r == null) {
                return null;
            } else if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                GetableResource p = fr.getHtml();
                if (p != null) {
                    indexPage = p;
                } else {
                    indexPage = fr;
                }
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
        Branch b = getBranch();
        if (b != null && b.getRepository() != null) {
            return b.getRepository().isPublicContent();
        } else {
            return false;
        }
    }

    @Override
    public Branch getBranch() {
        return getBranch(false);
    }

    @Override
    public Repository getRepository() {
        Branch b = getBranch();
        if (b != null) {
            return b.getRepository();
        } else {
            return null;
        }
    }

    @Override
    public String getHash() {
        Branch branch = getBranch();
        if (branch != null) {
            Commit c = branch.getHead();
            if (c == null) {
                return null;
            } else {
                return c.getItemHash();
            }
        } else {
            return null;
        }
    }

    @Override
    public void setHash(String s) {
        DataSession dataSession = getDataSession(true);
        dataSession.getRootDataNode().setHash(s);
        children = null;
    }

    @Override
    public Profile getModifiedBy() {
        Branch branch = getBranch();
        Commit h = null;
        if (branch != null) {
            h = branch.getHead();
        }
        if (h == null) {
            return null;
        } else {
            return h.getEditor();
        }
    }

    public DataSession getDataSession() {
        return getDataSession(false);
    }

    @Override
    public List<ContentDirectoryResource> getSubFolders() throws NotAuthorizedException, BadRequestException {
        List<ContentDirectoryResource> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if (r instanceof ContentDirectoryResource) {
                if (!r.getName().equals(".mil")) {
                    list.add((ContentDirectoryResource) r);
                }
            }
        }
        return list;
    }

    @Override
    public List<ContentResource> getFiles() throws NotAuthorizedException, BadRequestException {
        List<ContentResource> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if ((r instanceof ContentResource) && !(r instanceof ContentDirectoryResource)) {
                if (!r.getName().equals(".mil")) {
                    list.add((ContentResource) r);
                }
            }
        }
        return list;
    }

    @Override
    public Profile getOwnerProfile() {
        Branch b = getBranch();
        if (b != null) {
            BaseEntity be = b.getRepository().getBaseEntity();
            if (be instanceof Profile) {
                return (Profile) be;
            }
        }
        return null;
    }

    @Override
    public FileResource getOrCreateFile(String name) throws NotAuthorizedException, BadRequestException {
        Resource r = child(name);
        FileResource fr;
        if (r == null) {
            DataSession.FileNode newNode = getDirectoryNode().addFile(name);
            fr = new FileResource(newNode, this);
        } else {
            if (r instanceof FileResource) {
                fr = (FileResource) r;
            } else {
                throw new RuntimeException("Resource exists, but is not a FileResource: " + name + " is a " + r.getClass());
            }
        }
        return fr;
    }

    @Override
    public DirectoryResource getOrCreateDirectory(String name, boolean autoCreate) throws NotAuthorizedException, NotAuthorizedException, BadRequestException {
        Resource r = child(name);
        DirectoryResource fr;
        if (r == null) {
            if (autoCreate) {
                DataSession.DirectoryNode newNode = getDirectoryNode().addDirectory(name);
                fr = new DirectoryResource(newNode, this);
            } else {
                return null;
            }
        } else {
            if (r instanceof DirectoryResource) {
                fr = (DirectoryResource) r;
            } else {
                throw new RuntimeException("Resource exists, but is not a DirectoryResource: " + name + " is a " + r.getClass());
            }
        }
        return fr;
    }

    public boolean isLive() {
        String sBranch = this.getBranch().getRepository().getLiveBranch();
        if (sBranch == null) {
            return false;
        }
        return sBranch.equals(this.getName());
    }
}
