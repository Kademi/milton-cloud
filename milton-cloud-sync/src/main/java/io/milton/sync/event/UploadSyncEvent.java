package io.milton.sync.event;

import io.milton.event.Event;
import java.io.File;


/**
 *
 * @author brad
 */


public class UploadSyncEvent implements Event{
    private final File localFile;

    public UploadSyncEvent(File localFile) {
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }
    
    
}
