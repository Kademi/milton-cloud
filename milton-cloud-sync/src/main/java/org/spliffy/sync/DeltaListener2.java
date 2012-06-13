package org.spliffy.sync;

import io.milton.common.Path;
import java.io.IOException;
import io.milton.cloud.common.Triplet;

/**
 * A "delta" here refers to some difference between the client and server
 * file systems.
 * 
 *
 * @author brad
 */
public interface DeltaListener2 {

    
    void onLocalDeletion(Path path, Triplet remoteTriplet) throws IOException;
    
    void onLocalChange(Triplet localTriplet, Path path) throws IOException;

    void onRemoteChange(Triplet remoteTriplet, Triplet localTriplet, Path path) throws IOException;
    
    void onRemoteDelete(Triplet localTriplet, Path path) throws IOException;
    
    void onTreeConflict(Triplet remoteTriplet, Triplet localTriplet, Path path) throws IOException;

    void onFileConflict(Triplet remoteTriplet, Triplet localTriplet, Path path) throws IOException;
     
}
