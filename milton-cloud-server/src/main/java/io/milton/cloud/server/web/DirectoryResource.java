package io.milton.cloud.server.web;

import io.milton.cloud.common.ITriplet;
import io.milton.vfs.data.HashCalc;
import io.milton.vfs.db.ItemHistory;
import io.milton.vfs.db.MetaItem;
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
import io.milton.vfs.content.ContentSession;
import io.milton.vfs.content.ContentSession.DirectoryNode;
import io.milton.vfs.db.utils.SessionManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.hashsplit4j.api.Parser;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Represents a version of a directory, containing the members which are in that
 * directory in the repository snapshot
 *
 * @author brad
 */
public class DirectoryResource extends AbstractContentResource implements ContentDirectoryResource, PutableResource, GetableResource, ITriplet {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DirectoryResource.class);
    private final boolean renderMode;
    private ResourceList children;    

    public DirectoryResource(ContentSession.DirectoryNode directoryNode, ContentDirectoryResource parent, Services services, boolean renderMode) {
        super(directoryNode, parent, services);
        this.renderMode = renderMode;
    }


    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = Utils.toResources(this, members, renderMode);
        }
        return children;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        MetaItem newMeta = Utils.newDirItemVersion();
        DirectoryResource rdr = new DirectoryResource(newName, newMeta, this, services, renderMode);
        addChild(rdr);
        save(session);

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

        MetaItem newMeta = Utils.newFileItemVersion();
        FileResource fileResource = new FileResource(newName, newMeta, this, services);

        String ct = HttpManager.request().getContentTypeHeader();
        if (ct != null && ct.equals("spliffy/hash")) {
            // read the new hash and set it on this
            DataInputStream din = new DataInputStream(inputStream);

            long newHash = din.readLong();
            log.info("createNew: setting hash: " + hash);
            fileResource.setHash(newHash);
        } else {
            log.info("createNew: set content");
            // parse data and persist to stores
            Parser parser = new Parser();
            long fileHash = parser.parse(inputStream, getHashStore(), getBlobStore());

            fileResource.setHash(fileHash);
        }
        addChild(fileResource);
        save(session);
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
                HashCalc.calcResourceesHash(getChildren(), out); 
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
}
