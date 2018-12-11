package io.milton.sync.triplets;

import org.hashsplit4j.api.BlobStore;

/**
 *
 * @author brad
 */
public interface BlockingBlobStore extends BlobStore{
    /**
     * Implementations may block here to wait for async operations to complete
     *
     */
    void checkComplete() throws Exception;
}
