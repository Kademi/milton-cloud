package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Commit;
import io.milton.cloud.common.HashCalc;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.LessParameterParser;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.*;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Repository;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;

/**
 * Represents a branch in a repository
 *
 * This behaves much the same as a DirectoryResource but is defined differently
 *
 *
 * @author brad
 */
public class BranchFolder extends AbstractCollectionResource implements ContentDirectoryResource, PropFindableResource, MakeCollectionableResource, GetableResource, PutableResource, PostableResource, CopyableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    protected final CommonCollectionResource parent;
    protected final String name;
    protected final Branch branch;
    protected ResourceList children;
    protected Commit commit; // may be null
    protected final DataSession dataSession;
    protected JsonResult jsonResult; // set after completing a POST
    protected NodeMeta nodeMeta;
    private RenderFileResource indexPage;
    private Map<String, String> themeParams;
    private Map<String, String> themeAttributes;

    public BranchFolder(String name, CommonCollectionResource parent, Branch branch) {
        this.name = name;
        this.parent = parent;
        this.branch = branch;
        if (branch != null) {
            this.commit = branch.getHead();
        }
        this.dataSession = new DataSession(branch, SessionManager.session(), _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
    }

    @Override
    public String getUniqueId() {
        return "br" + branch.getId();
    }

    @Override
    public void save() throws IOException {
        String lastHash = null;
        Long lastId = null;
        if (branch != null && branch.getHead() != null) {
            lastId = branch.getHead().getId();
            lastHash = branch.getHead().getItemHash();
        }

        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("No current user!");
        }
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
            ApplicationManager am = _(ApplicationManager.class);
            children = am.toResources(this, dataSession.getRootDataNode());
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
        DirectoryNode newNode = dataSession.getRootDataNode().addDirectory(newName);
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
//        if (renderMode) {
//            if (getIndex() != null) {
//                getIndex().sendContent(out, null, params, null);
//                return;
//            }
//        }
        log.trace("sendContent: render template");
        WebUtils.setActiveMenu(getHref(), WebUtils.findRootFolder(this));
        MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos"); // For admin
        getTemplater().writePage(false, "myfiles/directoryIndex", this, params, out);
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
        String copyToName = WebUtils.getParam(parameters, "copyToName");
        log.info("processForm: " + copyToName);
        if( copyToName != null ) {             
            copyTo(getParent(), copyToName);
            String newHref = parent.getPath().child(copyToName).toString();
            jsonResult = new JsonResult(true, "Copied", newHref);
        }
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
        if ("branch".equals(type) ) {
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
        return branch.getRepository().isPublicContent();
    }

    @Override
    public Branch getBranch() {
        return branch;
    }

    @Override
    public String getHash() {
        Commit c = branch.getHead();
        if (c == null) {
            return null;
        } else {
            return c.getItemHash();
        }
    }

    @Override
    public void setHash(String s) {
        this.dataSession.getRootDataNode().setHash(s);
    }

    @Override
    public Profile getModifiedBy() {
        Commit h = branch.getHead();
        if (h == null) {
            return null;
        } else {
            return h.getEditor();
        }
    }

    public DataSession getDataSession() {
        return dataSession;
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
        BaseEntity be = b.getRepository().getBaseEntity();
        if (be instanceof Profile) {
            return (Profile) be;
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

    public String getPublicTheme() {
        return branch.getPublicTheme();
    }

    public List<String> getThemes() {
        List<String> list = new ArrayList<>(); // TODO: HACK!
        list.add("fuse");
        list.add("milton");
        list.add("custom");
        return list;
    }

    /**
     * Theme params are CSS variables (ie using LESS).
     * 
     * Reads the LESS file in /theme/theme-params.less and returns a map of
     * variable values
     *
     * @return
     */
    public Map<String, String> getThemeParams() {
        if (themeParams == null) {
            DataSession.DirectoryNode themeDir = (DataSession.DirectoryNode) this.getDirectoryNode().get("theme");
            themeParams = new HashMap<>();
            if (themeDir != null) {
                DataSession.FileNode paramsFile = (DataSession.FileNode) themeDir.get("theme-params.less");
                if (paramsFile != null) {
                    try {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        paramsFile.writeContent(bout);
                        InputStream in = new ByteArrayInputStream(bout.toByteArray());
                        LessParameterParser lessParameterParser = new LessParameterParser();
                        lessParameterParser.findParams(in, themeParams);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return themeParams;
    }

    /**
     * Merges the given map of variables into the theme-params.less file, or
     * creates if it doesnt exist
     * 
     * @param map
     * @throws IOException 
     */
    public void setThemeParams(Map<String, String> map) throws IOException {
        this.themeParams = null;

        DataSession.DirectoryNode themeDir = (DataSession.DirectoryNode) this.getDirectoryNode().get("theme");
        themeParams = new HashMap<>();
        if (themeDir != null) {
            DataSession.FileNode paramsFile = (DataSession.FileNode) themeDir.get("theme-params.less");
            InputStream bin = null;
            if (paramsFile == null) {
                paramsFile = themeDir.addFile("theme-params.less");
            } else {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                paramsFile.writeContent(bout);
                bin = new ByteArrayInputStream(bout.toByteArray());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            LessParameterParser lessParameterParser = new LessParameterParser();
            lessParameterParser.setParams(map, bin, out);
            paramsFile.setContent(new ByteArrayInputStream(out.toByteArray()));            
        }
    }
    

    /**
     * Theme attributes are things like logo text and additional menu items, which
     * are part of the theme but not CSS. These are stored in a properties
     * file in the theme folder
     * 
     * @param themeAttributes 
     */
    public void setThemeAttributes(Map<String, String> atts) throws IOException {
        this.themeAttributes = null;
        
        Properties props = new Properties();
        for( Map.Entry<String, String> entry : atts.entrySet() ) {
            props.setProperty(entry.getKey(), entry.getValue());
        }

        DataSession.DirectoryNode themeDir = (DataSession.DirectoryNode) this.getDirectoryNode().get("theme");
        if (themeDir == null) {
            themeDir = this.getDirectoryNode().addDirectory("theme");
        }
        
        DataSession.FileNode paramsFile = (DataSession.FileNode) themeDir.get("theme-attributes.properties");
        InputStream bin = null;
        if (paramsFile == null) {
            paramsFile = themeDir.addFile("theme-attributes.properties");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        paramsFile.setContent(new ByteArrayInputStream(out.toByteArray()));
        
    }    
    
    public Map<String, String> getThemeAttributes() {
        if (themeAttributes == null) {
            DataSession.DirectoryNode themeDir = (DataSession.DirectoryNode) this.getDirectoryNode().get("theme");
            themeAttributes = new HashMap<>();
            if (themeDir != null) {
                DataSession.FileNode paramsFile = (DataSession.FileNode) themeDir.get("theme-attributes.properties");
                if (paramsFile != null) {
                    try {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        paramsFile.writeContent(bout);
                        InputStream in = new ByteArrayInputStream(bout.toByteArray());
                        Properties props = new Properties();
                        props.load(in);
                        for( String key : props.stringPropertyNames() ) {
                            String val = props.getProperty(key);
                            themeAttributes.put(key, val);
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        return themeAttributes;
    }

    public boolean isLive() {
        String sBranch = this.branch.getRepository().getLiveBranch();
        if( sBranch == null ) {
            return false;
        }
        return sBranch.equals(this.getName());
    }

    @Override
    public void copyTo(CollectionResource toCollection, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
        log.info("createCollection: " + newName);
        if( !(toCollection instanceof RepositoryFolder)) {
            throw new BadRequestException("The target folder must be a repository folder to copy a branch. Is a: " + toCollection.getClass());
        }
        RepositoryFolder toFolder = (RepositoryFolder) toCollection;
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Repository toRepo = toFolder.getRepository();
        Branch newBranch;
        Date now = _(CurrentDateService.class).getNow();
        if( toRepo == branch.getRepository()) {
            newBranch = branch.copy(newName, now, session);
        } else {
            newBranch = branch.copy(toRepo, newName, now, session);
        }
        log.info("Created branch: " + newBranch.getId() + " with name: " + newBranch.getName());
        tx.commit();
    }
}
