package io.milton.sync.triplets;

import io.milton.common.Path;
import io.milton.sync.SwingConflictResolver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.triplets.ITriplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class FileUpdatingMergingDeltaListener implements DeltaGenerator.DeltaListener {

    private static final Logger log = LoggerFactory.getLogger(FileUpdatingMergingDeltaListener.class);

    private final File root;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private Long choiceTimeout;
    private Integer rememberSecs;
    private SwingConflictResolver.ConflictChoice choice;
    private SwingConflictResolver conflictResolver = new SwingConflictResolver();

    public FileUpdatingMergingDeltaListener() {
        this.root = null;
        this.hashStore = null;
        this.blobStore = null;
    }

    public FileUpdatingMergingDeltaListener(File root, HashStore hashStore, BlobStore blobStore) {
        this.root = root;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
    }

    @Override
    public void doDeleted(Path p, ITriplet triplet1) {
        File dir = find(p);
        File dest = new File(dir, triplet1.getName());
        log.info("deleted {}", dest.getAbsolutePath());
        if (dest.exists()) {
            try {
                if (dest.isDirectory()) {
                    FileUtils.deleteDirectory(dest);
                } else {
                    dest.delete();
                }
            } catch (IOException ex) {
                throw new RuntimeException("Could not delete: " + dest.getAbsolutePath(), ex);
            }

        }
    }

    @Override
    public void doUpdated(Path p, ITriplet triplet2) {
        File dir = find(p);
        File dest = new File(dir, triplet2.getName());
        log.info("updated {}", dest.getAbsolutePath());
        if (triplet2.getType().equals("d")) {
            // Just make sure it exists
            if (!dest.exists()) {
                dest.mkdirs();
            }
        } else {
            String fileHash = triplet2.getHash();
            Combiner c = new Combiner();
            Fanout fileFanout = hashStore.getFileFanout(fileHash);
            try (FileOutputStream fout = new FileOutputStream(dest)) {
                log.info("write local file: {}", dest.getAbsolutePath());
                c.combine(fileFanout.getHashes(), hashStore, blobStore, fout);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void doCreated(Path p, ITriplet triplet2) {
        File dir = find(p);
        File dest = new File(dir, triplet2.getName());
        log.info("created {}", dest.getAbsolutePath());
        if (triplet2.getType().equals("d")) {
            // Just make sure it exists
            if (!dest.exists()) {
                dest.mkdirs();
            }
        } else {
            String fileHash = triplet2.getHash();
            Combiner c = new Combiner();
            Fanout fileFanout = hashStore.getFileFanout(fileHash);
            try (FileOutputStream fout = new FileOutputStream(dest)) {
                log.info("write local file: {}", dest.getAbsolutePath());
                c.combine(fileFanout.getHashes(), hashStore, blobStore, fout);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private File find(Path p) {
        File f = root;
        for (String s : p.getParts()) {
            f = new File(f, s);
        }
        return f;
    }

    @Override
    public void doConflict(Path path, ITriplet triplet2) {
        log.info("onFileConflict: path={} ", path);
        File dir = find(path);
        File localChild = new File(dir, triplet2.getName());

        // Check if we have a non-expired timeout
        SwingConflictResolver.ConflictChoice n;
        if (this.choice != null && System.currentTimeMillis() < choiceTimeout) {
            n = choice;
        } else {
            String message = "Files are in conflict. There has been a change to a local file, but also a change to the corresponding remote file: " + localChild.getAbsolutePath();
            n = conflictResolver.showConflictResolver(message, rememberSecs);
            rememberSecs = conflictResolver.getRememberSecs();
            if (rememberSecs != null) {
                this.choice = n;
                choiceTimeout = System.currentTimeMillis() + conflictResolver.rememberSecs * 1000;
            }
        }
        if (n == SwingConflictResolver.ConflictChoice.LOCAL) {
            // do nothing, leave file as it is
        } else if (n == SwingConflictResolver.ConflictChoice.REMOTE) {
            // take the remote file and update local
            // todo: use a diff/merge tool like https://github.com/albfan/jmeld, or https://www.guiffy.com/help/GuiffyHelp/doc/com/guiffy/inside/GuiffyDiff.html
            doUpdated(path, triplet2);
        }
    }

}
