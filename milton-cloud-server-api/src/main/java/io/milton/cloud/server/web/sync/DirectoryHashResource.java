package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.resource.AccessControlledResource;
import org.apache.log4j.Logger;
import org.hashsplit4j.api.BlobStore;

/**
 *
 * @author brad
 */
public class DirectoryHashResource extends BaseResource implements GetableResource {

    private static final Logger log = Logger.getLogger(DirectoryHashResource.class);
    
    private final String hash;

    public DirectoryHashResource(String hash, SpliffySecurityManager securityManager, CommonCollectionResource parent) {
        super(securityManager, parent);
        this.hash = hash;
    }

    /**
     * The directory list is stored as a list of triplets as a blob, with the
     * hash being the hash of the triplet list. To send content we just look up
     * the blob and write it directly to the outputstream
     *
     * @param out
     * @param range
     * @param map
     * @param string
     * @throws IOException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     * @throws NotFoundException
     */
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (hash != null && hash.length() > 0) {
            byte[] dirList = _(BlobStore.class).getBlob(hash);
            if( dirList != null ) { // is null for an empty repository
                out.write(dirList);
            }            
        } else {
            log.warn("No hash, so no contents");
        }
    }

    @Override
    public String getName() {
        return hash + "";
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l; // 10 years
    }

    @Override
    public String getContentType(String string) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }
    
    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }     
}
