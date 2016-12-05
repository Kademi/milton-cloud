 package io.milton.sync;

import org.hashsplit4j.api.BlobStore;

/**
 * Always write to both
 *
 * getBlob reads from primary only
 *
 * hasBlob reads from secondary only
 *
 * @author brad
 */
public class WriteThroughBlobStore implements BlobStore {
    private final BlobStore primary;
    private final BlobStore secondary;

    public WriteThroughBlobStore(BlobStore primary, BlobStore secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public void setBlob(String hash, byte[] bytes) {
        primary.setBlob(hash, bytes);
        secondary.setBlob(hash, bytes);
    }

    @Override
    public byte[] getBlob(String hash) {
        byte[] arr = primary.getBlob(hash);
        if( arr == null ) {
            arr = secondary.getBlob(hash);
        }
        return arr;
    }

    @Override
    public boolean hasBlob(String hash) {
        return secondary.hasBlob(hash);
    }


}
