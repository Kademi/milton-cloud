package io.milton.cloud.server.web;

import io.milton.vfs.data.HashCalc;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import io.milton.vfs.content.ContentSession.DirectoryNode;
import io.milton.vfs.content.ContentSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Represents a version of a directory, containing the members which are in that
 * directory in the repository snapshot
 *
 * @author brad
 */
public class DirectoryResource extends AbstractContentResource implements ContentDirectoryResource, PutableResource, GetableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DirectoryResource.class);
    private final DirectoryNode directoryNode;
    private final boolean renderMode;
    private ResourceList children;

    public DirectoryResource(DirectoryNode directoryNode, ContentDirectoryResource parent, Services services, boolean renderMode) {
        super(directoryNode, parent, services);
        this.directoryNode = directoryNode;
        this.renderMode = renderMode;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = Utils.toResources(this, directoryNode, renderMode);
        }
        return children;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        DirectoryNode newNode = directoryNode.addDirectory(newName);
        DirectoryResource rdr = new DirectoryResource(newNode, this, services, renderMode);
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
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        String type = HttpManager.request().getParams().get("type");
        if (type == null) {
            // output directory listing
            getTemplater().writePage("directoryIndex", this, params, out);
        } else {
            if (type.equals("hashes")) {
                HashCalc.getInstance().calcHash(directoryNode.getDataNode(), out);
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
}
