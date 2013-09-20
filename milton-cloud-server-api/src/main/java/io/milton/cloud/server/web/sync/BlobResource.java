package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Request;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;

/**
 * Note that this outputs the bytes for a single chunk. Many such chunks will
 * usually make up a file
 *
 * @author brad
 */
public class BlobResource extends BaseResource implements GetableResource{

    private final byte[] blob;
    private final String hash;
    
    public BlobResource(byte[] blob, String hash, SpliffySecurityManager securityManager, CommonCollectionResource parent) {
        super(securityManager, parent);
        this.blob = blob;
        this.hash = hash;
    }        
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        out.write(blob);
        out.flush();
    }

    @Override
    public Long getContentLength() {
        return (long)blob.length;
    }

    @Override
    public String getUniqueId() {
        return hash;
    }

    @Override
    public String getName() {
        return hash;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l; // 10 years
    }

    @Override
    public String getContentType(String string) {
        return "application/octet-stream";
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    } 
}
