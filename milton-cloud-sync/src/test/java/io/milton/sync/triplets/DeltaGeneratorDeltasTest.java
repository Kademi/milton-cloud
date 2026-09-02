package io.milton.sync.triplets;

import io.milton.common.Path;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.store.MemoryBlobStore;
import org.hashsplit4j.store.MemoryHashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Two bugs, both visible to a person using ksync3:
 * <p>
 * The working directory listing was read from hash1 rather than workingDirHash, so
 * every entry's "has the local copy changed" test compared the base against itself.
 * The answer was always "yes", so a plain pull reported a conflict for every updated
 * file and prompted the user about a local change they had not made.
 * <p>
 * And an unchanged directory was walked anyway, reading and parsing every listing
 * below it to report nothing.
 *
 * @author claude
 */
public class DeltaGeneratorDeltasTest {

    static class T implements ITriplet {

        private final String name, hash, type;

        T(String name, String hash, String type) {
            this.name = name;
            this.hash = hash;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getHash() {
            return hash;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    /** Records what happened, and counts blob reads so pruning can be observed. */
    static class Recorder implements DeltaGenerator.DeltaListener {

        final List<String> events = new ArrayList<>();

        @Override
        public void doDeleted(Path p, ITriplet t) {
            events.add("deleted " + p.child(t.getName()));
        }

        @Override
        public void doUpdated(Path p, ITriplet t) {
            events.add("updated " + p.child(t.getName()));
        }

        @Override
        public void doCreated(Path p, ITriplet t) {
            events.add("created " + p.child(t.getName()));
        }

        @Override
        public void doConflict(Path p, ITriplet t) {
            events.add("conflict " + p.child(t.getName()));
        }
    }

    /** Counts getBlob calls, which is how an avoided subtree walk is detected. */
    static class CountingBlobStore extends MemoryBlobStore {

        int reads;

        @Override
        public byte[] getBlob(String hash) {
            reads++;
            return super.getBlob(hash);
        }
    }

    private final HashCalc hashCalc = HashCalc.getInstance();

    /** Stores a listing and returns its hash. */
    private String dir(BlobStore store, ITriplet... entries) throws Exception {
        List<ITriplet> list = new ArrayList<>(Arrays.asList(entries));
        hashCalc.sort(list);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String hash = hashCalc.calcHash(list, out);
        store.setBlob(hash, out.toByteArray());
        return hash;
    }

    private static final String H_A = "1111111111111111111111111111111111111111";
    private static final String H_B = "2222222222222222222222222222222222222222";
    private static final String H_C = "3333333333333333333333333333333333333333";

    private Recorder run(BlobStore store, String base, String target, String working) throws Exception {
        Recorder recorder = new Recorder();
        new DeltaGenerator(new MemoryHashStore(), store, recorder)
                .generateDeltas(base, target, working);
        return recorder;
    }

    /**
     * The local copy is untouched - it still matches the base - so taking the branch's
     * version loses nothing and this is an update, not a conflict.
     */
    @Test
    public void testUntouchedLocalFileIsUpdatedNotConflicted() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String base = dir(store, new T("a.txt", H_A, "f"));
        String target = dir(store, new T("a.txt", H_B, "f"));
        // The working tree holds the base version, ie the user has not edited it.
        String working = dir(store, new T("a.txt", H_A, "f"));

        assertEquals(Arrays.asList("updated /a.txt"), run(store, base, target, working).events);
    }

    /** Both sides changed it, so it is a genuine conflict. */
    @Test
    public void testLocallyEditedFileIsAConflict() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String base = dir(store, new T("a.txt", H_A, "f"));
        String target = dir(store, new T("a.txt", H_B, "f"));
        String working = dir(store, new T("a.txt", H_C, "f"));

        assertEquals(Arrays.asList("conflict /a.txt"), run(store, base, target, working).events);
    }

    /** The local copy already matches the target, so there is nothing to lose. */
    @Test
    public void testLocalAlreadyMatchingTargetIsNotAConflict() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String base = dir(store, new T("a.txt", H_A, "f"));
        String target = dir(store, new T("a.txt", H_B, "f"));
        String working = dir(store, new T("a.txt", H_B, "f"));

        assertEquals(Arrays.asList("updated /a.txt"), run(store, base, target, working).events);
    }

    @Test
    public void testCreatesAndDeletes() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String base = dir(store, new T("gone.txt", H_A, "f"));
        String target = dir(store, new T("added.txt", H_B, "f"));
        String working = dir(store, new T("gone.txt", H_A, "f"));

        List<String> events = run(store, base, target, working).events;
        assertTrue(events.toString(), events.contains("created /added.txt"));
        assertTrue(events.toString(), events.contains("deleted /gone.txt"));
        assertEquals(2, events.size());
    }

    /**
     * An unchanged directory is not walked. Its hash covers every descendant, so
     * nothing below it can differ - reading the subtree could only ever report nothing.
     */
    @Test
    public void testUnchangedSubtreeIsNotWalked() throws Exception {
        CountingBlobStore store = new CountingBlobStore();

        // A subtree of some depth that is identical on both sides.
        String deep = dir(store, new T("deep.txt", H_A, "f"));
        String middle = dir(store, new T("deep", deep, "d"));
        String untouched = dir(store, new T("middle", middle, "d"));

        String base = dir(store, new T("untouched", untouched, "d"), new T("a.txt", H_A, "f"));
        String target = dir(store, new T("untouched", untouched, "d"), new T("a.txt", H_B, "f"));
        String working = dir(store, new T("untouched", untouched, "d"), new T("a.txt", H_A, "f"));

        store.reads = 0;
        Recorder recorder = run(store, base, target, working);

        assertEquals("only the changed file should be reported",
                Arrays.asList("updated /a.txt"), recorder.events);
        // Three listings for the root (base, target, working) and nothing more: the
        // identical subtree's three levels are never opened.
        assertEquals("the unchanged subtree was walked", 3, store.reads);
    }

    /** A directory that is new in the target has no base listing, and must not NPE. */
    @Test
    public void testNewDirectoryIsReportedWithoutABaseListing() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String added = dir(store, new T("inside.txt", H_A, "f"));
        String base = dir(store, new T("a.txt", H_A, "f"));
        String target = dir(store, new T("a.txt", H_A, "f"), new T("added", added, "d"));
        String working = dir(store, new T("a.txt", H_A, "f"));

        List<String> events = run(store, base, target, working).events;
        assertTrue(events.toString(), events.contains("created /added"));
        assertTrue(events.toString(), events.contains("created /added/inside.txt"));
    }

    /** No working hash at all means no conflict detection is possible, so update. */
    @Test
    public void testNoWorkingHashUpdates() throws Exception {
        MemoryBlobStore store = new MemoryBlobStore();
        String base = dir(store, new T("a.txt", H_A, "f"));
        String target = dir(store, new T("a.txt", H_B, "f"));

        assertEquals(Arrays.asList("updated /a.txt"), run(store, base, target, null).events);
    }
}
