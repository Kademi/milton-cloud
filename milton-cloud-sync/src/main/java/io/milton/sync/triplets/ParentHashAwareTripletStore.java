package io.milton.sync.triplets;


import java.util.List;
import org.hashsplit4j.triplets.ITriplet;

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
    List<ITriplet> getTriplets(String hash);    
}
