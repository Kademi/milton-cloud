package io.milton.sync.triplets;

import java.io.File;
import java.io.IOException;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.store.MemoryBlobStore;
import org.hashsplit4j.store.MemoryHashStore;
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
        String hash2 = st2.scanDirectory(dir2);

        System.out.println("hash1=" + hash1 + " - hash2=" + hash2);

        boolean didDelete = false;
        boolean didCreate = false;

        DeltaGenerator dg = new DeltaGenerator(hashStore, blobStore, new DeltaGenerator.DeltaListener() {

            @Override
            public void doDeleted(ITriplet triplet1) {
                if( triplet1.getName().equals("a-deleted.txt")) {
                    Assert.assertEquals("a-deleted.txt", triplet1.getName());
                }
            }

            @Override
            public void doUpdated(ITriplet triplet2) {
                if( triplet2.getType().equals("f")) {
                    Assert.assertEquals("a-changed.txt", triplet2.getName());
                }
            }

            @Override
            public void doCreated(ITriplet triplet2) {
                if( triplet2.getType().equals("f")) {
                    Assert.assertEquals("a-new1.txt", triplet2.getName());
                }
            }
        });
        dg.generateDeltas(hash1, hash2);

    }

}
