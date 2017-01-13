package io.milton.sync;

import org.hashsplit4j.api.BlobStore;

/**
 *
 * @author brad
 */


public class MinimalPutsBlobStore implements BlobStore{

    private final BlobStore wrapped;

    public MinimalPutsBlobStore(BlobStore wrapped) {
        this.wrapped = wrapped;
    }
    
    
    
    @Override
    public void setBlob(String hash, byte[] bytes) {
        if( !wrapped.hasBlob(hash)) {
            wrapped.setBlob(hash, bytes);
        }
    }

    @Override
    public byte[] getBlob(String string) {
        if( wrapped.hasBlob(string)) {
            return wrapped.getBlob(string);
        }
        return null;
    }

    @Override
    public boolean hasBlob(String string) {
        return wrapped.hasBlob(string);
    }
    
}
