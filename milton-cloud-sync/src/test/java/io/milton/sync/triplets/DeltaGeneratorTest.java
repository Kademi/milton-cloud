package io.milton.sync.triplets;

import io.milton.common.Path;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.store.MemoryBlobStore;
import org.hashsplit4j.store.MemoryHashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author brad
 */
public class DeltaGeneratorTest {

    HashStore hashStore;
    BlobStore blobStore;
    private final HashCalc hashCalc = HashCalc.getInstance();

    public DeltaGeneratorTest() {
    }

    @Before
    public void setup() {
        hashStore = new MemoryHashStore();
        blobStore = new MemoryBlobStore();
    }

    @Test
    public void testScan() throws IOException {
        File dir1 = new File("src/test/resources/branch1");
        File dir2 = new File("src/test/resources/branch2");
        System.out.println("dir1: " + dir1.getAbsolutePath());

        MemoryLocalTripletStore st1 = new MemoryLocalTripletStore(dir1, blobStore, hashStore);
        String hash1 = st1.scanDirectory(dir1);

        MemoryLocalTripletStore st2 = new MemoryLocalTripletStore(dir2, blobStore, hashStore);
        String hash2 = st2.scanDirectory(dir2);

        System.out.println("hash1=" + hash1 + " - hash2=" + hash2);

        DeltaGenerator db2;

        DeltaGenerator dg = new DeltaGenerator(hashStore, blobStore, new DeltaGenerator.DeltaListener() {

            @Override
            public void doDeleted(Path p, ITriplet triplet1) {
                if( triplet1.getName().equals("a-deleted.txt")) {
                    Assert.assertEquals("a-deleted.txt", triplet1.getName());
                }
            }

            @Override
            public void doUpdated(Path p, ITriplet triplet2) {
                if( triplet2.getType().equals("f")) {
                    Assert.assertEquals("a-changed.txt", triplet2.getName());
                }
            }

            @Override
            public void doCreated(Path p, ITriplet triplet2) {
                if( triplet2.getType().equals("f")) {
                    Assert.assertEquals("a-new1.txt", triplet2.getName());
                }
            }

            @Override
            public void doConflict(Path path, ITriplet triplet2) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        dg.generateDeltas(hash1, hash2, null);

    }

    @Test
    public void testMerge() throws IOException {
        File dir1 = new File("src/test/resources/branch1");
        File dir2 = new File("src/test/resources/branch2");
        File mergeDest = new File("src/test/resources/branch3");

        MemoryLocalTripletStore st1 = new MemoryLocalTripletStore(dir1, blobStore, hashStore);
        String hash1 = st1.scanDirectory(dir1);

        MemoryLocalTripletStore st2 = new MemoryLocalTripletStore(dir2, blobStore, hashStore);
        String hash2 = st2.scanDirectory(dir2);

        MemoryLocalTripletStore stMergeDest = new MemoryLocalTripletStore(mergeDest, blobStore, hashStore);
        String origMergeDest = stMergeDest.scanDirectory(mergeDest);

        OneWayMergingDeltaListener dl = new OneWayMergingDeltaListener(origMergeDest, blobStore);

        DeltaGenerator dg = new DeltaGenerator(hashStore, blobStore,dl);
        System.out.println("Start merge: original branch: " + origMergeDest);
        dg.generateDeltas(hash1, hash2, null);

        System.out.println("result of merge: " + dl.getHash());

        System.out.println("Before merge: ");
        showBranch(origMergeDest, "");

        System.out.println("After merge:");
        showBranch(dl.getHash(), "");
    }

    private void showBranch(String hash, String indent) throws IOException {
        byte[] arr = blobStore.getBlob(hash);
        List<ITriplet> triplets = hashCalc.parseTriplets(new ByteArrayInputStream(arr));
        for( ITriplet t : triplets ) {
            System.out.println(indent + t.getName() + " - " + t.getType() + " - " + t.getHash());
            if( t.getType().equals("d")) {
                showBranch(t.getHash(), indent + "  ");
            }
        }
    }
}
