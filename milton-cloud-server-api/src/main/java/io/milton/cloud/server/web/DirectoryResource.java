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
import io.milton.cloud.server.web.NodeChildUtils.ResourceCreator;
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
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Profile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a version of a directory, containing the members which are in that
 * directory in the repository snapshot
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class DirectoryResource extends AbstractContentResource implements ContentDirectoryResource, PutableResource, GetableResource, ParameterisedResource, ResourceCreator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DirectoryResource.class);
    private final DirectoryNode directoryNode;
    private final boolean renderMode;
    private ResourceList children;
    private RenderFileResource indexPage;
    private boolean updatedIndex;

    public DirectoryResource(DirectoryNode directoryNode, ContentDirectoryResource parent, boolean renderMode) {
        super(directoryNode, parent);
        this.directoryNode = directoryNode;
        this.renderMode = renderMode;
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
            children = NodeChildUtils.toResources(this, directoryNode, renderMode, this);
            _(ApplicationManager.class).addBrowseablePages(this, children);
            children = children.getSortByName(); // ensure a stable and predictable sorting
        }
        return children;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        // Defensive check
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if( curUser == null ) {
            log.warn("req: " + HttpManager.request());
            throw new RuntimeException("No current user!!");
        }
        
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryNode newNode = directoryNode.addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this, renderMode);
        //rdr.updateModDate();
        onAddedChild(this);
        try {
            save();
        } catch (IOException ex) {
            throw new BadRequestException("io ex", ex);
        }

        tx.commit();

        return rdr;
    }

    @Override
    public void save() throws IOException {
        // Defensive check
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if( curUser == null ) {
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
        getTemplater().writePage("directoryIndex", this, params, out);
    }

    @Override
    public String getContentType(String accepts) {
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
        if (indexPage == null) {
            indexPage = getHtmlPage("index.html", true);
        }
        return indexPage;
    }

    public RenderFileResource getHtmlPage(String name, boolean autocreate) throws NotAuthorizedException, BadRequestException {
        RenderFileResource rfr;
        Resource r = child(name);
        if (r == null) {
            if( !autocreate ) {
                System.out.println("DirRes: not found and not autocreate");
                return null;
            }
            DataSession.FileNode newNode = getDirectoryNode().addFile(name);
            FileResource fr = new FileResource(newNode, this);
            rfr = fr.getHtml();
            rfr.setParsed(true);
            if (renderMode) {
                children.add(rfr);
            } else {
                children.add(fr);
            }
        } else if (r instanceof FileResource) {
            FileResource fr = (FileResource) r;
            rfr = fr.getHtml();
        } else if (r instanceof RenderFileResource) {
            rfr = (RenderFileResource) r;
        } else {
            return null;
        }
        System.out.println("DirRes: rfr=" + rfr);
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
        RenderFileResource r = getIndex();
        if (r != null) {
            updatedIndex = true;
            r.setTitle(s);
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
        RenderFileResource html = getIndex();
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
        if (updatedIndex = true) {
            RenderFileResource html = getIndex();
            if (html != null) {
                html.doSaveHtml();
            }
        }
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
        if (("folder".equals(type) || "directory".equals(type)) && !getName().startsWith(".")) {
            return true;
        }
        return super.is(type);
    }
}
