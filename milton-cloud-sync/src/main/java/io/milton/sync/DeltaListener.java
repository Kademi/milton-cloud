package io.milton.sync;

import io.milton.common.Path;
import java.io.IOException;
import org.hashsplit4j.triplets.ITriplet;

/**
 * A "delta" here refers to some difference between the client and server
 * file systems.
 * 
 *
 * @author brad
 */
public interface DeltaListener {

    
    void onLocalDeletion(Path path, ITriplet remoteTriplet) throws IOException;
    
    void onLocalChange(ITriplet localTriplet, Path path, ITriplet remoteTriplet) throws IOException;

    void onRemoteChange(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException;
    
    void onRemoteDelete(ITriplet localTriplet, Path path) throws IOException;
    
    void onTreeConflict(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException;

    void onFileConflict(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException;
     
}
