package io.milton.sync;

import java.io.File;
import org.hashsplit4j.api.FileBlobStore;
import io.milton.cloud.common.Triplet;

/**
 *
 * @author brad
 */
public class LocalFileTriplet extends Triplet {
    private final FileBlobStore blobStore;

    public LocalFileTriplet(File file) {
        blobStore = new FileBlobStore(file);
    }

    public FileBlobStore getBlobStore() {
        return blobStore;
    }
    
    public File getFile() {
        return blobStore.getFile(); 
    }
                
}
