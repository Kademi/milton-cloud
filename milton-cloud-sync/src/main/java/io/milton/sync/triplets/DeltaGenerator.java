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
        List<ITriplet> triplets1 = readListing(hash1, true);
        // A missing target listing is tolerated, as it always has been: the caller gets
        // deletes for everything in triplets1 and nothing else.
        List<ITriplet> triplets2 = readListing(hash2, false);
        // The working directory listing comes from workingDirHash. It used to be read
        // from hash1, which made tripletWorkingHash equal to the base hash for every
        // entry - so every changed file failed the "did the local copy change" test and
        // was reported as a conflict. In ksync3 that meant a plain pull prompted the
        // user about a local change they had not made, for every updated file.
        List<ITriplet> tripletsWorkingDir = readListing(workingDirHash, true);

        generateDeltas(triplets1, triplets2, tripletsWorkingDir, path);
    }

    /**
     * Reads and parses a directory listing blob. Null hash means no listing. A named
     * but absent blob throws when required, which is what the base and working
     * listings have always done - losing them silently would mean losing the
     * information conflict detection depends on.
     */
    private List<ITriplet> readListing(String hash, boolean required) throws IOException {
        if (hash == null) {
            return null;
        }
        byte[] blob = blobStore.getBlob(hash);
        if (blob == null) {
            if (required) {
                log.warn("Could not locate blob: {}", hash);
                throw new RuntimeException("Could not locate blob: " + hash + " in blob store: " + blobStore);
            }
            return null;
        }
        return hashCalc.parseTriplets(new ByteArrayInputStream(blob));
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
                } else if (triplet1.getHash().equals(triplet2.getHash())) {
                    // Identical, and for a directory that means every descendant is
                    // identical too - the hash covers the whole subtree, which is the
                    // point of hashing directories. So it is not walked. Without this
                    // an unchanged subtree was read and parsed all the way down to
                    // report nothing.
                    log.trace("Unchanged, not walking {}/{} hash={}", path, triplet1.getName(), triplet1.getHash());
                    continue;
                } else {
                    // we only support conflict handling for files
                    if (triplet1.getType().equals("d")
                            || tripletWorkingHash == null
                            || triplet2.getHash().equals(tripletWorkingHash)
                            // The local copy still matches the base, so it has not been
                            // edited and there is nothing to lose by updating it. Absent
                            // this, every clean update was a conflict.
                            || triplet1.getHash().equals(tripletWorkingHash)) {
                        deltaListener.doUpdated(path, triplet2);
                    } else {
                        deltaListener.doConflict(path, triplet2);
                    }
                }

                if (triplet2.getType().equals("d")) {
                    // triplet1 is null for a directory that is new in the target, and
                    // then the whole subtree is reported as created.
                    String triplet1Hash = triplet1 == null ? null : triplet1.getHash();
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
