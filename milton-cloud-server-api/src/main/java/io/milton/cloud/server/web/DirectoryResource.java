package io.milton.cloud.server.web;

import io.milton.cloud.server.web.NodeChildUtils.ResourceCreator;
import io.milton.vfs.data.HashCalc;
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

    public DirectoryResource(DirectoryNode directoryNode, ContentDirectoryResource parent, boolean renderMode) {
        super(directoryNode, parent);
        this.directoryNode = directoryNode;
        this.renderMode = renderMode;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = services.getApplicationManager().getPage(this, childName);
        if (r != null) {
            return r;
        }        
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = NodeChildUtils.toResources(this, directoryNode, renderMode, this);
            System.out.println("DirectoryResource.getChildren: " + getName() + " - " + children.size());
            services.getApplicationManager().addBrowseablePages(this, children);
            System.out.println("DirectoryResource.getChildren: " + getName() + " - " + children.size());
        }
        return children;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryNode newNode = directoryNode.addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this, renderMode);
        rdr.updateModDate(); 
        onAddedChild(this);
        save();

        tx.commit();

        return rdr;
    }

    @Override
    public void save() {
        parent.save();
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        FileNode newFileNode = directoryNode.addFile(newName);
        FileResource fileResource = newFileResource(newFileNode, this, false);

        String ct = HttpManager.request().getContentTypeHeader();
        if (ct != null && ct.equals("spliffy/hash")) {
            // read the new hash and set it on this
            DataInputStream din = new DataInputStream(inputStream);
            long newHash = din.readLong();
            newFileNode.setHash(newHash);
            fileResource.updateModDate();
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
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        String type = HttpManager.request().getParams().get("type");
        if (type == null) {
            // output directory listing
            getTemplater().writePage("directoryIndex", this, params, out);
        } else {
            if (type.equals("hashes")) {
                HashCalc.getInstance().calcHash(directoryNode, out);
            } else if (type.equals("hash")) {
                String s = directoryNode.getHash() + "";
                out.write(s.getBytes());
            }
        }
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
        Resource r = child("index.html");
        if (r == null) {
            return null;
        } else if (r instanceof FileResource) {
            FileResource fr = (FileResource) r;
            return fr.getHtml();
        } else if (r instanceof RenderFileResource) {
            return (RenderFileResource) r;
        } else {
            return null;
        }
    }

    public String getTitle() throws NotAuthorizedException, BadRequestException {
        RenderFileResource r = getIndex();
        if (r != null) {
            return r.getTitle();
        } else {
            return getName();
        }
    }

    public void setTitle(String s) throws NotAuthorizedException, BadRequestException {
        RenderFileResource r = getIndex();
        if (r != null) {
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
        if (html == null) {
            // create a new one
            throw new RuntimeException("not done yet");
        }
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
        RenderFileResource html = getIndex();
        if (html != null) {
            html.doCommit(knownProps, errorProps);
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
        if(("folder".equals(type) || "directory".equals(type)) && !getName().startsWith(".") ) {
            return true;
        }
        return super.is(type);
    }
    
    
}
