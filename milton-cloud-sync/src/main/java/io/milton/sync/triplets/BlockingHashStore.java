package io.milton.sync.triplets;

import org.hashsplit4j.api.HashStore;

/**
 *
 * @author brad
 */
public interface BlockingHashStore extends HashStore{
    /**
     * Implementations may block here to wait for async operations to complete
     *
     */
    void checkComplete() throws Exception;
}
