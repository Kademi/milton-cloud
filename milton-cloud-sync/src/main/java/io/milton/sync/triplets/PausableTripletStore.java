 package io.milton.sync.triplets;

/**
 *
 * @author brad
 */


public interface PausableTripletStore extends TripletStore {
    void setPaused(boolean b);
}
