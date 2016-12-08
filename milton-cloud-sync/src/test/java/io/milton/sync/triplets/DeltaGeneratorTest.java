package io.milton.sync.triplets;

import java.io.File;
import java.io.IOException;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.store.MemoryBlobStore;
import org.hashsplit4j.store.MemoryHashStore;
import org.hashsplit4j.triplets.ITriplet;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author brad
 */
public class DeltaGeneratorTest {

    HashStore hashStore;
    BlobStore blobStore;

    public DeltaGeneratorTest() {
    }

    @Before
    public void setup() {
        hashStore = new MemoryHashStore();
        blobStore = new MemoryBlobStore();
    }

    @Test
    public void testSomeMethod() throws IOException {
        File dir1 = new File("src/test/resources/branch1");
        File dir2 = new File("src/test/resources/branch2");
        System.out.println("dir1: " + dir1.getAbsolutePath());

        MemoryLocalTripletStore st1 = new MemoryLocalTripletStore(dir1, blobStore, hashStore);
        String hash1 = st1.scanDirectory(dir1);

        MemoryLocalTripletStore st2 = new MemoryLocalTripletStore(dir2, blobStore, hashStore);
        String hash2 = st1.scanDirectory(dir2);

        System.out.println("hash1=" + hash1 + " - hash2=" + hash2);

        DeltaGenerator dg = new DeltaGenerator(hashStore, blobStore, new DeltaGenerator.DeltaListener() {

            @Override
            public void doDeleted(ITriplet triplet1) {
                System.out.println("deleted: " + triplet1.getName());
            }

            @Override
            public void doUpdated(ITriplet triplet2) {
                System.out.println("updated: " + triplet2.getName());
            }

            @Override
            public void doCreated(ITriplet triplet2) {
                System.out.println("created: " + triplet2.getName());
            }
        });
        dg.generateDeltas(hash1, hash2);

    }

}
