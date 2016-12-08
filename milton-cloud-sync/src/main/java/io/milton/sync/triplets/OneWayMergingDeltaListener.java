package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.sync.triplets.DeltaGenerator.DeltaListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;

/**
 *
 * @author brad
 */
public class OneWayMergingDeltaListener implements DeltaListener{

    private final String startingHash;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private String hash;
    private final HashCalc hashCalc = HashCalc.getInstance();

    public OneWayMergingDeltaListener(String startingHash, HashStore hashStore, BlobStore blobStore) {
        this.startingHash = startingHash;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.hash = startingHash;
    }




    @Override
    public void doDeleted(Path p, ITriplet triplet1) {
        this.hash = findAndRemove(p, triplet1.getName());
        if( triplets != null ) {
            Iterator<Triplet> it = triplets.iterator();
            while( it.hasNext() ) {
                Triplet t = it.next();
                if( t.getName().equals(triplet1.getName())) {
                    it.remove();
                }
            }
            // hmm, now how do we calc a new root hash?
        }

    }

    @Override
    public void doUpdated(Path p, ITriplet triplet2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doCreated(Path p, ITriplet triplet2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String findAndRemove(Path p, String name) {
        findAndRemote(Path.root, name);
    }

    private String findAndRemove(Path p, String name) {
        if( p.isRoot() ) {
            return getTriplets(hash);
        } else {
            List<ITriplet> parentTriplets = findAndRemove(p.getParent());
            if( parentTriplets == null ) {
                return null;
            }
            for( ITriplet t : parentTriplets) {
                if( t.getName().equals(p.getName())) {
                    return getTriplets(t.getHash());
                }
            }
            return null;
        }
    }

    private List<ITriplet> getTriplets(String hash) {
        byte[] dir = blobStore.getBlob(hash);
        try {
            return hashCalc.parseTriplets(new ByteArrayInputStream(dir));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
