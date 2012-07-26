package io.milton.sync.triplets;

import io.milton.cloud.common.Triplet;
import java.util.List;

/**
 *
 * @author brad
 */


public interface ParentHashAwareTripletStore extends TripletStore {
    /**
     * Get the list of triplets when the directory hash is known
     * 
     * @param hash
     * @return 
     */
    List<Triplet> getTriplets(long hash);    
}
