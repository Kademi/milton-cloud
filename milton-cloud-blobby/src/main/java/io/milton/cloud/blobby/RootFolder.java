package io.milton.cloud.blobby;

import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import java.util.Collections;
import java.util.List;

/**
 * This folder allows blobs to be written directly to the blob store,
 * independently of the file from whence they came
 *
 * @author brad
 */
class RootFolder extends BaseResource implements CollectionResource {

    private final BlobFolder blobFolder;
    
    public RootFolder(BlobbyResourceFactory resourceFactory) {
        super(resourceFactory);
        blobFolder = new BlobFolder(resourceFactory.getBlobStore(), "blobs", resourceFactory);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Resource child(String name) throws NotAuthorizedException, BadRequestException {
        if( name.equals(blobFolder.getName() )) {
            return blobFolder;
        } else {
            return null;
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }
}
