package io.milton.sync.triplets;

import io.milton.common.Path;
import java.util.List;
import io.milton.cloud.common.Triplet;

/**
 * A Triplet describes a member within a directory/collection, but its
 * name, hash, and type (and metaId, but lets ignore that because it
 * makes it a Quadlet which sounds crap)
 * 
 * A list of Triplets defines the state of a directory/collection
 * 
 * This interface defines a means of getting a list of triplets for a local
 * or remote directory
 *
 * @author brad
 */
public interface TripletStore {
    /**
     * Return the list of triplets for the given path (which might be
     * relative to a root location)
     * 
     * @param path
     * @return - the list of triplets defining the state of the directory, or null
     * if the directory was not found
     */
    List<Triplet> getTriplets(Path path);
    
}
