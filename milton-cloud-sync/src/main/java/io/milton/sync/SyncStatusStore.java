package io.milton.sync;

import io.milton.common.Path;

/**
 * Represents a means of recording what version of a resource was last synced
 *
 * @author brad
 */
public interface SyncStatusStore {

    String findBackedUpHash(Path path);

    void setBackedupHash(Path path, String hash);

    /**
     * Called when the syncronisation has removed a file
     *
     * @param path
     */
    void clearBackedupHash(Path path);
}
