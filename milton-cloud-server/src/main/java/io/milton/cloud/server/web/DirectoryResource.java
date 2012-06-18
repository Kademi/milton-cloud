package io.milton.cloud.server.web;

import io.milton.vfs.data.HashCalc;
import io.milton.vfs.db.ItemHistory;
import io.milton.vfs.db.MetaItem;
import io.milton.vfs.db.SessionManager;
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
public class DirectoryResource extends AbstractMutableResource implements PutableResource, GetableResource, MutableCollection {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DirectoryResource.class);
    private final boolean renderMode;
    private ResourceList children;    
    private boolean dirty;

    public DirectoryResource(String name, MetaItem meta, MutableCollection parent, Services services, boolean renderMode) {
        super(name, meta, parent, services);
        this.renderMode = renderMode;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
        if (toCollection instanceof MutableCollection) {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();

            MutableCollection newParent = (MutableCollection) toCollection;
            MetaItem newMeta = Utils.newFileItemVersion();
            DirectoryResource newDir = new DirectoryResource(newName, newMeta, newParent, services, renderMode);
            newDir.setHash(hash);
            newParent.addChild(newDir);
            newParent.save(session);
            tx.commit();
        } else {
            throw new ConflictException(this, "Can't copy to collection of type: " + toCollection.getClass());
        }
    }

    @Override
    public void removeChild(MutableResource r) throws NotAuthorizedException, BadRequestException {
        setDirty(true);
        getChildren().remove(r);
        parent.onChildChanged(this);
    }

    @Override
    public void addChild(MutableResource r) throws NotAuthorizedException, BadRequestException {
        log.trace("addChild: " + getName());
        Resource existing = child(r.getName());
        if (existing != null) {
            MutableResource mr = (MutableResource) existing;
            removeChild(mr);
        }
        setDirty(true);
        getChildren().add(r);
        parent.onChildChanged(this);
    }

    @Override
    public void onChildChanged(MutableResource r) {
        setDirty(true);
        parent.onChildChanged(this);
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            if (getItemVersion() != null) {
                List<ItemHistory> members = getItemVersion().getMembers();
                children = Utils.toResources(this, members, renderMode);
            } else {
                children = new ResourceList();
            }
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

    /**
     * Just call up to the RepoResource. It will call back to generate and save
     * hashes for all directories, then it will save a new RepoVersion
     *
     * @param session
     * @return
     */
    @Override
    public void save(Session session) {
        parent.save(session);
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
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        log.trace("setDirty: " + dirty + "  on : " + getName());
        this.dirty = dirty;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public String getType() {
        return "d";
    }

    @Override
    public void setEntryHash(long hash) {
        this.hash = hash;
    }
}
