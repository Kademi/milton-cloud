package io.milton.sync.triplets;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hashsplit4j.store.MemoryBlobStore;
import org.hashsplit4j.store.MemoryHashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This scanner hashed directories in File.listFiles order, so the hash was not a
 * function of the content. The server stores an uploaded hash verbatim, so an unsorted
 * listing stuck to a branch for good.
 *
 * @author claude
 */
public class MemoryLocalTripletStoreSortTest {

    private static void write(File f, String content) throws Exception {
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes("UTF-8"));
        }
    }

    private static List<String> names(List<ITriplet> triplets) {
        List<String> names = new ArrayList<>();
        for (ITriplet t : triplets) {
            names.add(t.getName());
        }
        return names;
    }

    private String scan(File root, MemoryBlobStore blobStore) throws Exception {
        MemoryLocalTripletStore store = new MemoryLocalTripletStore(root, null, blobStore,
                new MemoryHashStore(), null, null, null, null, new MemorySyncHashCache());
        try {
            return store.scan();
        } finally {
            store.stop();
        }
    }

    @Test
    public void testDirectoryListingIsSorted() throws Exception {
        File root = Files.createTempDirectory("triplet-sort").toFile();
        // Created out of alphabetical order, and enough of them to make a chance pass
        // unlikely on a filesystem that returns entries in index order.
        String[] names = {"zebra.txt", "apple.txt", "mango.txt", "banana.txt",
            "cherry.txt", "yak.txt", "kiwi.txt", "date.txt", "elder.txt", "fig.txt"};
        for (int i = 0; i < names.length; i++) {
            write(new File(root, names[i]), "content " + i);
        }
        // A subdirectory too: a triplet sorts by name and type.
        write(new File(root, "sub/one.txt"), "1");
        write(new File(root, "sub/two.txt"), "2");

        MemoryBlobStore blobStore = new MemoryBlobStore();
        String rootHash = scan(root, blobStore);
        assertNotNull("scan returned no hash", rootHash);

        byte[] listing = blobStore.getBlob(rootHash);
        assertNotNull("no listing blob stored for the root hash", listing);

        HashCalc hashCalc = HashCalc.getInstance();
        List<ITriplet> asStored = hashCalc.parseTriplets(new ByteArrayInputStream(listing));
        assertEquals("wrong number of entries", 11, asStored.size());

        List<ITriplet> sorted = new ArrayList<>(asStored);
        hashCalc.sort(sorted);
        assertEquals("the stored listing is not in sorted order", names(sorted), names(asStored));

        // And the hash is of that sorted listing - what the server would compute.
        assertEquals("the root hash is not the hash of the sorted listing",
                hashCalc.calcHash(sorted), rootHash);
    }

    /** The property that matters: identical content, identical hash, whatever listFiles says. */
    @Test
    public void testSameContentSameHashWhateverTheCreationOrder() throws Exception {
        File forwards = Files.createTempDirectory("triplet-fwd").toFile();
        File backwards = Files.createTempDirectory("triplet-bwd").toFile();
        String[] names = {"apple.txt", "banana.txt", "cherry.txt", "date.txt",
            "elder.txt", "fig.txt", "kiwi.txt", "mango.txt", "yak.txt", "zebra.txt"};

        for (String name : names) {
            write(new File(forwards, name), "content of " + name);
        }
        List<String> reversed = new ArrayList<>(Arrays.asList(names));
        Collections.reverse(reversed);
        for (String name : reversed) {
            write(new File(backwards, name), "content of " + name);
        }

        assertEquals("the same content hashed differently depending on creation order",
                scan(forwards, new MemoryBlobStore()), scan(backwards, new MemoryBlobStore()));
    }
}
