package io.milton.cloud.server.apps.email;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.AbstractContentResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.DirectoryResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NewPageResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author brad
 */
public class ManageGroupEmailsFolder extends AbstractCollectionResource implements GetableResource, PostableResource, ContentDirectoryResource {
    private static final Logger log = LoggerFactory.getLogger(ManageGroupEmailsFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation org;
    private ResourceList children;
    private JsonResult jsonResult;
    private Branch _branch;
    private DataSession dataSession;
    private DataSession.DirectoryNode dirNode;

    public static Branch getGroupEmailBranch(Organisation org, boolean autocreate) {
        Repository groupEmailsRepo = org.repository("groupEmails");
        if (groupEmailsRepo == null && autocreate) {
            Profile user = _(SpliffySecurityManager.class).getCurrentUser();
            groupEmailsRepo = org.createRepository("groupEmails", user, SessionManager.session());
        }
        if (groupEmailsRepo != null) {
            Branch b = groupEmailsRepo.liveBranch();
            if( b == null ) {
                log.warn("branch is null. autocreate=" + autocreate);
            }
            if( b == null && autocreate ) {
                Profile user = _(SpliffySecurityManager.class).getCurrentUser();
                Session session = SessionManager.session();
                b = groupEmailsRepo.createBranch(Branch.TRUNK, user, session);
                groupEmailsRepo.setLiveBranch(b.getName());
                session.save(groupEmailsRepo);
                session.save(b);
                log.info("Created new branch: " + b.getName());
            }
            return b;
        } else {
            System.out.println("DID NOT CREATE group emails repo");
            return null;
        }
    }

    public ManageGroupEmailsFolder(String name, CommonCollectionResource parent, Organisation org) {
        this.name = name;
        this.parent = parent;
        this.org = org;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        // this form post is to create a shell group email job
        String nameToCreate = parameters.get("name");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        GroupEmailJob job = new GroupEmailJob();
        job.setTitle(nameToCreate);
        String nm = NewPageResource.findAutoCollectionName(nameToCreate, parent, parameters);
        job.setName(nm);
        job.setOrganisation(org);
        Date now = _(CurrentDateService.class).getNow();
        job.setStatusDate(now);
        session.save(job);
        tx.commit();

        jsonResult = new JsonResult(true);
        ManageGroupEmailFolder newFolder = new ManageGroupEmailFolder(null, job, this);
        jsonResult.setNextHref(newFolder.getHref());

        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuEmails", "menuSendEmail");
            _(HtmlTemplater.class).writePage("admin", "email/manageGroupEmail", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<GroupEmailJob> jobs = GroupEmailJob.findByOrg(org, SessionManager.session());
            dirNode = getOrCreateDirNode(false);
            for (GroupEmailJob f : jobs) {
                DirectoryNode emailDirNode = null;
                if (dirNode != null) {
                    emailDirNode = (DirectoryNode) dirNode.get(f.getName());
                    if( emailDirNode == null ) {
                        log.warn("Didnt find group email dir node: " + f.getName() + " in " + dirNode.getName());
                    }
                }
                ManageGroupEmailFolder faf = new ManageGroupEmailFolder(emailDirNode, f, this);
                children.add(faf);
            }
        }
        return children;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
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
    public DirectoryNode getDirectoryNode() {
        return getOrCreateDirNode(true);
    }

    @Override
    public void onAddedChild(AbstractContentResource aThis) {
    }

    @Override
    public void onRemovedChild(AbstractContentResource aThis) {
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
    public FileResource getOrCreateFile(String name) throws NotAuthorizedException, BadRequestException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectoryResource getOrCreateDirectory(String name, boolean autoCreate) throws NotAuthorizedException, NotAuthorizedException, BadRequestException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHash() {
        if (getBranch() != null && getBranch().getHead() != null) {
            return getBranch().getHead().getItemHash();
        } else {
            return null;
        }
    }

    @Override
    public void setHash(String s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Profile getModifiedBy() {
        if (getBranch() != null && getBranch().getHead() != null) {
            return getBranch().getHead().getEditor();
        } else {
            return null;
        }
    }

    @Override
    public void save() throws IOException {
        Branch branch = branch(true);
        if (branch == null) {
            throw new RuntimeException("Couldnt create the branch ... weird");
        }
        String lastHash = null;
        Long lastId = null;
        if (getBranch().getHead() != null) {
            lastId = getBranch().getHead().getId();
            lastHash = getBranch().getHead().getItemHash();
        }

        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("No current user!");
        }
        getOrCreateDirNode(true);
        log.info("is dirty? " + dataSession.getRootDataNode().isDirty());
        dataSession.save(currentUser);

        System.out.println("----------------------------------");
        System.out.println("branch head ID: " + getBranch().getHead().getId());
        System.out.println("branch head hash: " + getBranch().getHead().getItemHash());
        System.out.println("last hash: " + lastHash);
        System.out.println("Last head id: " + lastId);
        System.out.println("----------------------------------");
    }

    @Override
    public Branch getBranch() {
        return branch(false);
    }

    @Override
    public Repository getRepository() {
        if (getBranch() != null) {
            return getBranch().getRepository();
        } else {
            return null;
        }
    }

    @Override
    public Profile getOwnerProfile() {
        Branch b = branch(false);
        if (b == null) {
            return null;
        }
        BaseEntity be = b.getRepository().getBaseEntity();
        if (be instanceof Profile) {
            return (Profile) be;
        }
        return null;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Branch branch(boolean autocreate) {
        if (_branch == null) {
            System.out.println("get or create branch");
            _branch = getGroupEmailBranch(getOrganisation(), autocreate);
        }
        return _branch;
    }

    public DataSession.DirectoryNode getOrCreateDirNode(boolean autoCreate) {
        if (dirNode == null) {
            Branch rewardsBranch = branch(autoCreate);
            if (rewardsBranch != null) {
                dataSession = new DataSession(rewardsBranch, SessionManager.session(), _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
                dirNode = (DataSession.DirectoryNode) dataSession.getRootDataNode();
                System.out.println("------------- init data session -------------- " + dataSession.hashCode());
            }
        }
        return dirNode;
    }
}
