package io.milton.sync.triplets;

import io.milton.common.Path;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            dest.delete();
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
        for( String s : p.getParts()) {
            f = new File(f, s);
        }
        return f;
    }

}
