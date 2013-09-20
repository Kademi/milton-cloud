package io.milton.cloud.server.web.sync;

import io.milton.cloud.common.FanoutSerializationUtils;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.hashsplit4j.api.HashStore;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Request;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.PutableResource;
import io.milton.vfs.db.utils.SessionManager;
import org.apache.log4j.Logger;
import org.hashsplit4j.api.Fanout;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Allows fanouts to be uploaded and saved to the HashStore, independently of
 * any directory
 *
 * @author brad
 */
public class FanoutFolder extends BaseResource implements PutableResource , CommonCollectionResource{

    private static Logger log = Logger.getLogger(FanoutFolder.class);
    private final HashStore hashStore;
    private final String name;
    private final boolean isChunk; // or file

    public FanoutFolder(HashStore hashStore, String name, SpliffySecurityManager securityManager, boolean isChunk, CommonCollectionResource parent) {
        super(securityManager, parent);
        this.hashStore = hashStore;
        this.name = name;
        this.isChunk = isChunk;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Create a new chunk Fanout in the HashStore
     *
     * @param newName
     * @param inputStream
     * @param length
     * @param contentType
     * @return
     * @throws IOException
     * @throws ConflictException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        String hash = newName;
        log.info("createNew: set hash: " + newName);
        Fanout fanout = FanoutSerializationUtils.readFanout(inputStream);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (isChunk) {
            hashStore.setChunkFanout(hash, fanout.getHashes(), fanout.getActualContentLength());
        } else {
            hashStore.setFileFanout(hash, fanout.getHashes(), fanout.getActualContentLength());
        }
        tx.commit();
        return new FanoutResource(fanout, hash, securityManager, this);
    }

    @Override
    public Resource child(String string) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }
    
    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }     
}
