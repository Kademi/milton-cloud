package io.milton.cloud.blobby;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.milton.resource.GetableResource;
import java.util.Date;

/**
 * Note that this outputs the bytes for a single chunk. Many such chunks will
 * usually make up a file
 *
 * @author brad
 */
public class BlobResource extends BaseResource implements GetableResource{

    private final String hash;
    private final byte[] blob;
    
    public BlobResource(String hash, byte[] blob, BlobbyResourceFactory resourceFactory) {
        super(resourceFactory);
        this.hash = hash;
        this.blob = blob;
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
    public Date getModifiedDate() {
        return BlobbyResourceFactory.LONG_LONG_AGO;
    }
    
    
}
