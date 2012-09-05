package io.milton.cloud.blobby;

import io.milton.cloud.common.HashCalc;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.io.*;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.BlobStore;
import io.milton.resource.PutableResource;

/**
 * This folder allows blobs to be written directly to the blob store, independently
 * of the file from whence they came
 *
 * @author brad
 */
class BlobFolder extends  BaseResource implements PutableResource {

    private final BlobStore blobStore;
    private final String name;
    private final BlobbyResourceFactory resourceFactory;
    
    public BlobFolder(BlobStore blobStore, String name, BlobbyResourceFactory resourceFactory) {
        super(resourceFactory);
        this.blobStore = blobStore;
        this.resourceFactory = resourceFactory;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Create a new blob with the hash being the name of this resource, and the
     * contents being raw bytes
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
        ByteArrayOutputStream bout = new ByteArrayOutputStream(length.intValue());
        long actualBytes = IOUtils.copyLarge(inputStream, bout);
        if( length != null && !length.equals(actualBytes)) {
            throw new RuntimeException("Blob is not of expected length: expected=" + length + " actual=" + actualBytes);
        }
        byte[] bytes = bout.toByteArray();
        
        // Verify that the given hash does match the data
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        HashCalc.getInstance().verifyHash(bin, newName); // will throw exception if not valid
        
        blobStore.setBlob(newName, bytes);
        return new BlobResource(newName, bytes, resourceFactory);
    }

    @Override
    public Resource child(String hash) throws NotAuthorizedException, BadRequestException {
        byte[] blob = blobStore.getBlob(hash);
        if( blob == null ) {
            return null;
        } else {
            return new BlobResource(hash, blob, resourceFactory);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }
}
