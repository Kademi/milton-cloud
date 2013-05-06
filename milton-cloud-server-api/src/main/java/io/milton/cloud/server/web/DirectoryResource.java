/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.db.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Represents a version of a directory, containing the members which are in that
 * directory in the repository snapshot
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class DirectoryResource<P extends ContentDirectoryResource> extends AbstractContentResource<DirectoryNode, P> implements ContentDirectoryResource, PutableResource, GetableResource, ParameterisedResource, PostableResource, DeletableCollectionResource, TitledPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DirectoryResource.class);
    protected DirectoryNode directoryNode;
    protected ResourceList children;
    protected RenderFileResource indexPage;
    protected boolean updatedIndex;
    protected JsonResult jsonResult;

    public DirectoryResource(DirectoryNode directoryNode, P parent) {
        super(directoryNode, parent);
        this.directoryNode = directoryNode;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        if (parameters.containsKey("importFromUrl")) {
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
        return null;
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
            initChildren();
        }
        return children;
    }

    protected void initChildren() {
        ApplicationManager am = _(ApplicationManager.class);
        children = am.toResources(this, directoryNode);
        if (directoryNode != null) {
            if (log.isTraceEnabled()) {
                log.trace("initChildren: " + getName() + " children=" + children.size() + " - " + directoryNode.size());
            }
        } else {
            log.warn("Cant load children, directory node is null: " + getHref());
        }
        am.addBrowseablePages(this, children);
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        // Defensive check
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (curUser == null) {
            log.warn("req: " + HttpManager.request());
            throw new RuntimeException("No current user!!");
        }

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        DirectoryResource rdr = createDirectoryResource(newName);

        tx.commit();

        return rdr;
    }

    public DirectoryResource createDirectoryResource(String newName) throws BadRequestException {
        if (directoryNode == null) {
            directoryNode = parent.getDirectoryNode().addDirectory(getName());
            this.contentNode = directoryNode;
        }
        if (directoryNode.get(newName) != null) {
            throw new BadRequestException(this, "Resource with that name already exists: " + newName);
        }
        DirectoryNode newNode = directoryNode.addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this);
        rdr.updateModDate();
        onAddedChild(this);
        try {
            save();
        } catch (IOException ex) {
            throw new BadRequestException("io ex", ex);
        }
        return rdr;
    }

    @Override
    public void save() throws IOException {
        // Defensive check
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (curUser == null) {
            throw new RuntimeException("No current user!!");
        }
        parent.save();
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        return UploadUtils.createNew(this, newName, inputStream, length, contentType);
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
        } else {
            renderPage(out, params, contentType);
        }
    }

    public void renderPage(OutputStream out, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        RootFolder rf = WebUtils.findRootFolder(this);
        if (rf instanceof WebsiteRootFolder) {
            WebUtils.setActiveMenu(getHref(), rf); // For front end        
        } else {
            MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos"); // For admin
        }
        getTemplater().writePage("myfiles/directoryIndex", this, params, out);

    }

    @Override
    public String getContentType(String accepts) {
        if (jsonResult != null) {
            return JsonResult.CONTENT_TYPE;
        } else {
            String type = HttpManager.request().getParams().get("type");
            if (type == null || type.length() == 0) {
                return "text/html";
            } else {
                if (type.equals("hashes")) {
                    return "text/plain";
                } else {
                    return type;
                }
            }
        }
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public DirectoryNode getDirectoryNode() {
        if (directoryNode == null) {
            log.info("dir node is null, so create: " + getName());
            DataNode existing = parent.getDirectoryNode().get(getName());
            if (existing == null) {
                directoryNode = parent.getDirectoryNode().addDirectory(getName());
            } else {
                if (existing instanceof DirectoryNode) {
                    directoryNode = (DirectoryNode) existing;
                } else {
                    throw new RuntimeException("Found a node which is not a directory: " + getHref());
                }
            }
        }
        return directoryNode;
    }

    @Override
    public void onAddedChild(AbstractContentResource r) {
        try {
            getChildren().add(r);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onRemovedChild(AbstractContentResource r) {
        try {
            getChildren().remove(r);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderFileResource getIndex() throws NotAuthorizedException, BadRequestException {
        return getIndex(false);
    }

    public RenderFileResource getIndex(boolean autoCreate) throws NotAuthorizedException, BadRequestException {
        if (indexPage == null) {
            indexPage = getHtmlPage("index.html", autoCreate);
        }
        return indexPage;
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

    public RenderFileResource getHtmlPage(String name, boolean autocreate) throws NotAuthorizedException, BadRequestException {
        RenderFileResource rfr;
        Resource r = child(name);
        if (r == null) {
            if (!autocreate) {
                return null;
            }
            DataSession.FileNode newNode = getDirectoryNode().addFile(name);
            FileResource fr = new FileResource(newNode, this);
            rfr = fr.getHtml();
            rfr.setParsed(true);
            children.add(fr);
        } else if (r instanceof FileResource) {
            FileResource fr = (FileResource) r;
            if (autocreate) {
                rfr = fr.parseHtml();
            } else {
                rfr = fr.getHtml(); // only get parsed version if file is suitable
            }
        } else if (r instanceof RenderFileResource) {
            rfr = (RenderFileResource) r;
        } else {
            rfr = null;
        }
        if (autocreate && rfr == null) {
            throw new RuntimeException("Couldnt autocreate new html page. A resource exists of an incompatible type: " + r.getClass() + " name=" + name);
        }

        return rfr;
    }

    @Override
    public String getTitle() {
        try {
            RenderFileResource r = getIndex();
            String t = null;
            if (r != null) {
                t = r.getTitle();
            }
            if (t == null) {
                t = getName();
            }
            return t;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setTitle(String s) throws NotAuthorizedException, BadRequestException {
        RenderFileResource r = getIndex(true);
        if (r != null) {
            updatedIndex = true;
            r.setTitle(s);
        } else {
            throw new RuntimeException("no index page");
        }
    }

    public String getBody() {
        try {
            RenderFileResource r = getIndex();
            if (r != null) {
                return r.getBody();
            } else {
                return "";
            }
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setBody(String s) throws NotAuthorizedException, BadRequestException {
        RenderFileResource r = getIndex(true);
        if (r != null) {
            updatedIndex = true;
            r.setBody(s);
        } else {
            throw new RuntimeException("no index page");
        }
    }

    @Override
    public String getParam(String name) throws NotAuthorizedException, BadRequestException {
        RenderFileResource html = getIndex();
        if (html == null) {
            return null;
        } else {
            return html.getParam(name);
        }
    }

    @Override
    public void setParam(String name, String value) throws NotAuthorizedException, BadRequestException {
        RenderFileResource html = getIndex(true);
        updatedIndex = true;
        html.setParam(name, value);
    }

    @Override
    public List<String> getParamNames() throws NotAuthorizedException, BadRequestException {
        RenderFileResource html = getIndex();
        if (html == null) {
            return Collections.EMPTY_LIST;
        } else {
            return html.getParamNames();
        }

    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            doSaveHtml();
        } catch (IOException ex) {
            throw new BadRequestException("io ex", ex);
        }

        tx.commit();
    }

    /**
     * Save any parameters which have been set to HTML content
     */
    public void doSaveHtml() throws NotAuthorizedException, BadRequestException, IOException {
        if (updatedIndex == true) {
            RenderFileResource html = getIndex();
            if (html != null) {
                html.doSaveHtml();
            } else {
                log.warn("No html page to save");
            }
        } else {
            log.debug("not updatedIndex ---");
        }
    }

    @Override
    public boolean is(String type) {
        if (("folder".equals(type) || "directory".equals(type)) && !getName().startsWith(".")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public Branch getBranch() {
        return parent.getBranch();
    }

    @Override
    public String getHash() {
        return directoryNode.getHash();
    }

    @Override
    public void setHash(String s) {
        directoryNode.setHash(s);
    }

    @Override
    public List<ContentDirectoryResource> getSubFolders() throws NotAuthorizedException, BadRequestException {
        List<ContentDirectoryResource> list = new ArrayList<>();
        for (Resource r : getChildren()) {
            if (!r.getName().equals(".mil")) {
                if (r instanceof ContentDirectoryResource) {
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
                ContentResource cr = (ContentResource) r;
                list.add(cr);
            }
        }
        return list;
    }

    @Override
    public boolean isLockedOutRecursive(Request request) {
        return false; // TODO: implement proper lock check
    }
}
