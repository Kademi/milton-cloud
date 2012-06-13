package io.milton.cloud.server.db.store;

import java.util.List;
import io.milton.cloud.server.db.VolumeInstance;

/**
 * Controls the algorithm for selecting the next volume instance to write to,
 * and to read from
 *
 * @author brad
 */
public interface VolumeInstanceAllocator {

    /**
     * Find a VolumeInstance to write a new blob to. The optional previouslyFailed
     * parameter is used to indicate any VI's which have previously been attempted
     * (within the current http operation) so should be ignored
     * 
     * @param previouslyFailed
     * @return - null if no acceptable VI can be located, otherwise the preferred
     * VI to write to
     */
    public VolumeInstance nextWriteInstance(List<VolumeInstance> previouslyFailed);

    /**
     * Finds the best VI to read bytes from for the given volumeId. The
     * optional previouslyFailed parameter is to list any VI's which have been
     * used, but failed, in the current operation
     * 
     * @param volumeId
     * @param previouslyFailed
     * @return - null if no acceptable VI can be found, otherwise the best VI to read
     * from for the given volume
     */
    public VolumeInstance getReaderInstance(long volumeId, List<VolumeInstance> previouslyFailed);
    
}
