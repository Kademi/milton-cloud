package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.sync.triplets.DeltaGenerator.DeltaListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;

/**
 *
 * @author brad
 */
public class OneWayMergingDeltaListener implements DeltaListener {

    private final String startingHash;
    private final BlobStore blobStore;
    private String hash;
    private final HashCalc hashCalc = HashCalc.getInstance();

    public OneWayMergingDeltaListener(String startingHash, BlobStore blobStore) {
        this.startingHash = startingHash;
        this.hash = startingHash;

        this.blobStore = blobStore;
        this.hash = startingHash;
    }

    @Override
    public void doDeleted(Path p, ITriplet triplet1) {
        this.hash = modifyTriplets(p, (List<ITriplet> t) -> {
            Triplet child = findChildHash(triplet1.getName(), t);
            t.remove(child);
            return calcHash(t);

        });
    }

    @Override
    public void doUpdated(Path p, ITriplet triplet2) {
        if (triplet2.getType().equals("d")) {
            // Just make sure it exists
            this.hash = modifyTriplets(p, (List<ITriplet> t) -> {
                Triplet child = findChildHash(triplet2.getName(), t);
                if (child == null) {
                    System.out.println("Creating new dir triplet: " + triplet2.getName() + " in parent path: " + p);
                    child = new Triplet();
                    child.setName(triplet2.getName());
                    child.setType(triplet2.getType());
                    t.add(child);
                }
                return calcHash(t);
            });
        } else {
            this.hash = modifyTriplets(p, (List<ITriplet> t) -> {
                Triplet child = findChildHash(triplet2.getName(), t);
                if (child != null) {
                    child.setHash(triplet2.getHash());
                } else {
                    System.out.println("Creating new triplet: " + triplet2.getName() + " in parent path: " + p);
                    child = new Triplet();
                    child.setName(triplet2.getName());
                    child.setHash(triplet2.getHash());
                    child.setType(triplet2.getType());
                    t.add(child);
                }
                return calcHash(t);
            });
        }
    }

    @Override
    public void doCreated(Path p, ITriplet triplet2) {
        this.hash = modifyTriplets(p, (List<ITriplet> t) -> {
            System.out.println("add child: " + triplet2.getName());
            Triplet child = new Triplet();
            child.setName(triplet2.getName());
            child.setHash(triplet2.getHash());
            child.setType(triplet2.getType());
            t.add(child);
            return calcHash(t);
        });
    }

    private String modifyTriplets(Path p, Function<List<ITriplet>, String> f) {
        if (p.isRoot()) {
            List<ITriplet> triplets = getTriplets(hash);
            return f.apply(triplets);
        } else {
            return modifyTriplets(p.getParent(), (List<ITriplet> t) -> {
                Triplet child = findChildHash(p.getName(), t);
                List<ITriplet> triplets = getTriplets(child.getHash());
                String newHash = f.apply(triplets);
                child.setHash(newHash);
                return calcHash(t);
            });
        }
    }

    private List<ITriplet> getTriplets(String hash) {
        byte[] dir = blobStore.getBlob(hash);
        try {
            String sDir = new String(dir);
            System.out.println("Dir - " + hash);
            System.out.println(sDir);
            System.out.println("---");
            return hashCalc.parseTriplets(new ByteArrayInputStream(dir));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Triplet findChildHash(String child, List<ITriplet> triplets) {
        for (ITriplet t : triplets) {
            if (t.getName().equals(child)) {
                return (Triplet) t;
            }
        }
        return null;
    }

    private String calcHash(List<ITriplet> list) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String hash;
        try {
            hash = hashCalc.calcHash(list, bout);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        byte[] blob = bout.toByteArray();
        String s = new String(blob);
        //System.out.println(s);
        blobStore.setBlob(hash, blob);
        return hash;
    }

    public String getHash() {
        return hash;
    }

    public String getStartingHash() {
        return startingHash;
    }

    @Override
    public void doConflict(Path path, ITriplet triplet2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
