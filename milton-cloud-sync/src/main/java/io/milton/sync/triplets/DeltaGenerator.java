package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.sync.Utils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given two root hashes, one representing an earlier state and the other a
 * later state, generate a series of callbacks with the changes in the second
 * state relative to the first
 *
 * @author brad
 */
public class DeltaGenerator {

    private static final Logger log = LoggerFactory.getLogger(DeltaGenerator.class);

    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final HashCalc hashCalc = HashCalc.getInstance();
    private final DeltaListener deltaListener;

    private boolean canceled;

    public DeltaGenerator(HashStore hashStore, BlobStore blobStore, DeltaListener deltaListener) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.deltaListener = deltaListener;
    }

    public void generateDeltas(String hash1, String hash2) throws IOException {
        generateDeltas(hash1, hash2, Path.root);
    }

    public void generateDeltas(String hash1, String hash2, Path path) throws IOException {
        // find the dir listing for each hash
        List<ITriplet> triplets1 = null;
        if (hash1 != null) {
            byte[] dir1 = blobStore.getBlob(hash1);
            triplets1 = hashCalc.parseTriplets(new ByteArrayInputStream(dir1));
        }

        byte[] dir2 = blobStore.getBlob(hash2);
        List<ITriplet> triplets2 = hashCalc.parseTriplets(new ByteArrayInputStream(dir2));

        generateDeltas(triplets1, triplets2, path);
    }

    private void generateDeltas(List<ITriplet> triplets1, List<ITriplet> triplets2, Path path) throws IOException {
        if (canceled) {
            log.trace("walk canceled");
            return;
        }
        Map<String, ITriplet> tripletMap1 = Utils.toMap(triplets1);
        Map<String, ITriplet> tripletMap2 = Utils.toMap(triplets2);

        for (ITriplet triplet2 : triplets2) {
            if (canceled) {
                return;
            }
            ITriplet triplet1 = tripletMap1.get(triplet2.getName());
            if (triplet1 == null) {
                deltaListener.doCreated(path, triplet2);
            } else {
                if (triplet1.getHash().equals(triplet2.getHash())) {
                    // clean, nothing to do
                } else {
                    deltaListener.doUpdated(path, triplet2);
                }
            }

            if (triplet2.getType().equals("d")) {
                String triplet1Hash = null;
                if (triplet1 != null) {
                    triplet1Hash = triplet1.getHash();
                }
                generateDeltas(triplet1Hash, triplet2.getHash(), path.child(triplet2.getName()));
            }
        }

        // Now look for old resources which do not match (by name) new resources, these are deletes
        if (triplets1 != null) {
            for (ITriplet triplet1 : triplets1) {
                if (!tripletMap2.containsKey(triplet1.getName())) {
                    deltaListener.doDeleted(path, triplet1);
                }
            }
        }

    }

    public interface DeltaListener {

        void doDeleted(Path p, ITriplet triplet1);

        void doUpdated(Path p, ITriplet triplet2);

        void doCreated(Path p, ITriplet triplet2);

    }
}
