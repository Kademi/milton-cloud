package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Commit;
import io.milton.cloud.common.HashCalc;
import io.milton.cloud.server.DataSessionManager;
import io.milton.cloud.server.apps.website.LessParameterParser;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Repository;
import java.net.URI;
import java.net.URISyntaxException;
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
public class BranchFolder extends AbstractBranchFolder implements MakeCollectionableResource, GetableResource, PutableResource, PostableResource, CopyableResource, TitledPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    protected final Branch branch;
    protected Commit commit; // may be null
    protected final DataSession dataSession;
    protected JsonResult jsonResult; // set after completing a POST
    protected NodeMeta nodeMeta;
    private Map<String, String> themeParams;
    private Map<String, String> themeAttributes;

    public BranchFolder(String name, CommonCollectionResource parent, Branch branch) {
        super(name, parent);
        this.branch = branch;
        if (branch != null) {
            this.commit = branch.getHead();
            this.dataSession = _(DataSessionManager.class).get(branch);
        } else {
            this.dataSession = null;
        }
    }

    public BranchFolder(String name, CommonCollectionResource parent, Commit commit) {
        super(name, parent);
        this.branch = null;
        this.commit = commit;
        this.dataSession = new DataSession(commit, SessionManager.session(), _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
    }

    @Override
    public NodeMeta getNodeMeta() {
        return loadNodeMeta();
    }

    @Override
    public Branch getBranch(boolean createIfNeeded) {
        return branch;
    }

    @Override
    public DataSession getDataSession(boolean createIfNeeded) {
        return dataSession;
    }

    
    
    @Override
    public String getUniqueId() {
        if (branch != null) {
            return super.getUniqueId();
        } else {
            return "cm" + commit.getId();
        }
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


    @Override
    public Date getModifiedDate() {
        if( branch != null) {
            if( branch.getHead() != null ) {
                return branch.getHead().getCreatedDate();
            } else {
                return branch.getCreatedDate();
            }
        } else {
            return commit.getCreatedDate();
        }
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
        if (params.containsKey("importStatus")) {
            Profile p = _(SpliffySecurityManager.class).getCurrentUser();
            if (p != null) {
                Importer importer = Importer.getImporter(p, this);
                if (importer != null) {
                    jsonResult = new JsonResult(true);
                    jsonResult.setData(importer);
                } else {
                    jsonResult = new JsonResult(false, "Importer not found ");
                }
            } else {
                jsonResult = new JsonResult(false, "Not logged in");
            }
        }
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
        ContentRedirectorPage.select(this);
        RootFolder rf = WebUtils.findRootFolder(this);
        if (rf instanceof WebsiteRootFolder) {
            WebUtils.setActiveMenu(getHref(), rf); // For front end        
        } else {
            MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos"); // For admin
        }

        getTemplater().writePage("myfiles/directoryIndex", this, params, out);
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
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        String copyToName = WebUtils.getParam(parameters, "copyToName");
        log.info("processForm: " + copyToName);
        if (copyToName != null) {
            copyTo(getParent(), copyToName);
            String newHref = parent.getPath().child(copyToName).toString();
            jsonResult = new JsonResult(true, "Copied", newHref);
        } else if (parameters.containsKey("importFromUrl")) {
            String importFromUrl = WebUtils.getParam(parameters, "importFromUrl");
            log.info("Start import from url: " + importFromUrl);
            Profile p = _(SpliffySecurityManager.class).getCurrentUser();
            if (p != null) {
                try {
                    URI uri = new URI(importFromUrl);
                    Importer importer = Importer.create(p, this, uri);
                    importer.doImport();
                    jsonResult = new JsonResult(true);
                    jsonResult.setData(importer);
                } catch (URISyntaxException ex) {
                    jsonResult = new JsonResult(false, "Invalid url: " + importFromUrl + " Please enter something like http://domain.com/folder/file");
                }
            }
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
    public String getTitle() {
        if (branch != null) {
            return this.branch.getRepository().getTitle();
        } else {
            if (commit != null) {
                if (commit.getBranch() != null) {
                    return commit.getBranch().getRepository().getTitle();
                }
            }
            return "Unknown branch";
        }
    }


    @Override
    public Branch getBranch() {
        if (branch != null) {
            return branch;
        } else {
            return commit.getBranch();
        }
    }

    public Commit getCommit() {
        if (branch != null) {
            return branch.getHead();
        } else {
            return commit;
        }
    }

    @Override
    public String getHash() {
        if (branch != null) {
            Commit c = branch.getHead();
            if (c == null) {
                return null;
            } else {
                return c.getItemHash();
            }
        } else {
            return commit.getItemHash();
        }
    }


    @Override
    public Profile getModifiedBy() {
        Commit h;
        if (branch != null) {
            h = branch.getHead();
        } else {
            h = commit;
        }
        if (h == null) {
            return null;
        } else {
            return h.getEditor();
        }
    }

    public String getPublicTheme() {
        return getBranch().getPublicTheme();
    }

    public List<String> getThemes() {
        List<String> list = new ArrayList<>(); // TODO: HACK!
        list.add("fuse");
        list.add("milton");
        list.add("bootstrap");
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
        DataSession.DirectoryNode themeDir = (DataSession.DirectoryNode) this.getDirectoryNode().get("theme");
        if (themeDir == null) {
            themeDir = this.getDirectoryNode().addDirectory("theme");
        }

        themeParams = map;

        // Read the current file content, if any
        DataSession.FileNode paramsFile = (DataSession.FileNode) themeDir.get("theme-params.less");
        InputStream bin = null;
        if (paramsFile == null) {
            paramsFile = themeDir.addFile("theme-params.less");
        } else {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            paramsFile.writeContent(bout);
            bin = new ByteArrayInputStream(bout.toByteArray());
        }

        // Now merge new variables map with existing content
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LessParameterParser lessParameterParser = new LessParameterParser();
        lessParameterParser.setParams(map, bin, out);

        // And finally write the new file content
        paramsFile.setContent(new ByteArrayInputStream(out.toByteArray()));

    }

    /**
     * Theme attributes are things like logo text and additional menu items,
     * which are part of the theme but not CSS. These are stored in a properties
     * file in the theme folder
     *
     * @param themeAttributes
     */
    public void setThemeAttributes(Map<String, String> atts) throws IOException {
        this.themeAttributes = null;

        Properties props = new Properties();
        for (Map.Entry<String, String> entry : atts.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if( val == null ) {
                val = "";
            }
            props.setProperty(key, val);
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
                        for (String key : props.stringPropertyNames()) {
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

    @Override
    public boolean isLive() {
        String sBranch = this.getBranch().getRepository().getLiveBranch();
        if (sBranch == null) {
            return false;
        }
        return sBranch.equals(this.getName());
    }

    @Override
    public void copyTo(CollectionResource toCollection, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
        log.info("createCollection: " + newName);
        if (!(toCollection instanceof RepositoryFolder)) {
            throw new BadRequestException("The target folder must be a repository folder to copy a branch. Is a: " + toCollection.getClass());
        }
        RepositoryFolder toFolder = (RepositoryFolder) toCollection;
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Repository toRepo = toFolder.getRepository();
        Branch newBranch;
        Date now = _(CurrentDateService.class).getNow();
        if (toRepo == branch.getRepository()) {
            newBranch = branch.copy(newName, now, session);
        } else {
            newBranch = branch.copy(toRepo, newName, now, session);
        }
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        for (AppControl ac : AppControl.find(branch, session)) {
            ac.copyTo(newBranch, currentUser, now, session);
        }
        log.info("Created branch: " + newBranch.getId() + " with name: " + newBranch.getName());
        tx.commit();
    }
}
