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

    /**
     * Generate deltas from hash1 to hash2, with workingDirHash as a reference
     *
     * Files changed from hash1 to hash2, which are also different in
     * workingDirHash are considered a conflict.
     *
     * @param hash1
     * @param hash2
     * @param workingDirHash
     * @throws IOException
     */
    public void generateDeltas(String hash1, String hash2, String workingDirHash) throws IOException {
        generateDeltas(hash1, hash2, workingDirHash, Path.root);
    }

    public void generateDeltas(String hash1, String hash2, String workingDirHash, Path path) throws IOException {
        log.info("generateDeltas path={}", path);
        // find the dir listing for each hash
        List<ITriplet> triplets1 = null;
        if (hash1 != null) {
            byte[] dir1 = blobStore.getBlob(hash1);
            if (dir1 != null) {
                triplets1 = hashCalc.parseTriplets(new ByteArrayInputStream(dir1));
            } else {
                log.warn("Could not locate blob: {}", hash1);
                throw new RuntimeException("Could not locate blob: " + hash1 + " in blob store: " + blobStore);
            }
        }

        byte[] dir2 = blobStore.getBlob(hash2);
        List<ITriplet> triplets2 = null;
        if (dir2 != null) {
            triplets2 = hashCalc.parseTriplets(new ByteArrayInputStream(dir2));
        }

        List<ITriplet> tripletsWorkingDir = null;
        if (workingDirHash != null) {
            byte[] dir1 = null;
            if (hash1 != null) {
                dir1 = blobStore.getBlob(hash1);

                if (dir1 != null) {
                    tripletsWorkingDir = hashCalc.parseTriplets(new ByteArrayInputStream(dir1));
                } else {
                    log.warn("Could not locate blob: {}", hash1);
                    throw new RuntimeException("Could not locate blob: " + hash1 + " in blob store: " + blobStore);
                }
            }
        }

        generateDeltas(triplets1, triplets2, tripletsWorkingDir, path);
    }

    private void generateDeltas(List<ITriplet> triplets1, List<ITriplet> triplets2, List<ITriplet> tripletsWorkingDir, Path path) throws IOException {
        if (canceled) {
            log.trace("walk canceled");
            return;
        }
        Map<String, ITriplet> tripletMap1 = Utils.toMap(triplets1);
        Map<String, ITriplet> tripletMap2 = Utils.toMap(triplets2);
        Map<String, ITriplet> tripletMapWorking = Utils.toMap(tripletsWorkingDir);

        if (triplets2 != null) {
            for (ITriplet triplet2 : triplets2) {
                if (canceled) {
                    return;
                }
                ITriplet triplet1 = tripletMap1.get(triplet2.getName());

                ITriplet tripletWorking = tripletMapWorking.get(triplet2.getName());
                String tripletWorkingHash = null;
                if (tripletWorking != null) {
                    tripletWorkingHash = tripletWorking.getHash();
                }

                if (triplet1 == null) {
                    deltaListener.doCreated(path, triplet2);
                } else {
                    if (triplet1.getHash().equals(triplet2.getHash())) {
                        // clean, nothing to do
                        log.info("Directory={}/{} hashes match={}", path, triplet1.getName(), triplet1.getHash());
                    } else {
                        // we only support conflict handling for files
                        if (triplet1.getType().equals("d") || tripletWorkingHash == null || triplet2.getHash().equals(tripletWorkingHash)) {
                            deltaListener.doUpdated(path, triplet2);
                        } else {
                            deltaListener.doConflict(path, triplet2);
                        }
                    }
                }

                if (triplet2.getType().equals("d")) {
                    String triplet1Hash = null;
                    if (triplet1 != null) {
                        triplet1Hash = triplet1.getHash();
                    }
                    generateDeltas(triplet1Hash, triplet2.getHash(), tripletWorkingHash, path.child(triplet2.getName()));
                }
            }
        }

        // Now look for old resources which do not match (by name) new resources, these are deletes
        log.info("Check for deletes: {}", path);
        if (triplets1 != null) {
            for (ITriplet triplet1 : triplets1) {
                if (!tripletMap2.containsKey(triplet1.getName())) {
                    deltaListener.doDeleted(path, triplet1);
                } else {
                    log.info("Triplet1 {} exists in both", triplet1.getName());
                }
            }
        }

    }

    public interface DeltaListener {

        void doDeleted(Path p, ITriplet triplet1);

        void doUpdated(Path p, ITriplet triplet2);

        void doCreated(Path p, ITriplet triplet2);

        void doConflict(Path path, ITriplet triplet2);

    }
}
