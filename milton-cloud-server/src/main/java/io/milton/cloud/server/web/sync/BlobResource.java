package io.milton.cloud.server.web.sync;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.resource.GetableResource;

/**
 *
 * @author brad
 */
public class BlobResource extends BaseResource implements GetableResource{

    private final byte[] blob;
    private final long hash;
    
    public BlobResource(byte[] blob, long hash, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
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
        return hash + "";
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
        return "application/octet-stream";
    }


}
