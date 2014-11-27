package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.ConflictException;
import io.milton.sync.triplets.TripletStore;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SyncingDeltaListener implements DeltaListener {

    private static final Logger log = LoggerFactory.getLogger(SyncingDeltaListener.class);
    private final Syncer syncer;
    private final Archiver archiver;
    private final File root;
    private final SyncStatusStore syncStatusStore;
    private final TripletStore localTripletStore;
    private boolean readonlyLocal;

    public SyncingDeltaListener(Syncer syncer, Archiver archiver, File localRoot, SyncStatusStore syncStatusStore, TripletStore localTripletStore) {
        this.syncer = syncer;
        this.archiver = archiver;
        this.root = localRoot;
        this.syncStatusStore = syncStatusStore;
        this.localTripletStore = localTripletStore;
    }

    @Override
    public void onRemoteChange(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException {        
        if (Triplet.isDirectory(remoteTriplet)) {
            final File localFile = toFile(path);
            if (!localFile.exists()) {
                if (readonlyLocal) {
                    return;
                }
                log.info("onRemoteChange: Create local dir: {}", localFile.getAbsolutePath());
                if (!localFile.mkdirs()) {
                    throw new IOException("Couldnt create local directory: " + localFile.getAbsolutePath());
                }
                localTripletStore.refreshDir(path);
            } else {
                log.info("Local already exists: " + localFile.getAbsolutePath());
            }
        } else {
            final File localChild = toFile(path);
            if( localChild.exists() ) {
                log.info("modified remote file: " + localChild.getAbsolutePath() + " remote:" + remoteTriplet.getHash() + " != " + localTriplet.getHash());
            } else {
                log.info("new remote file: " + localChild.getAbsolutePath());
            }            
            if (readonlyLocal) {
                log.info("in read only mode so not doing anything");
                return;
            }
            syncer.downloadSync(remoteTriplet.getHash(), path);
            syncStatusStore.setBackedupHash(path, remoteTriplet.getHash());
        }
    }

    @Override
    public void onRemoteDelete(ITriplet localTriplet, Path path) {
        final File localChild = toFile(path);
        if (readonlyLocal) {
            return;
        }
        log.info("Archiving remotely deleted file: " + localChild.getAbsolutePath());        
        archiver.archive(localChild);
        syncStatusStore.clearBackedupHash(path);
    }

    @Override
    public void onLocalChange(ITriplet localTriplet, Path path) throws IOException {
        final File localFile = toFile(path);
        if (localFile.isFile()) {
            log.info("upload locally new or modified file: " + localFile.getCanonicalPath());
            syncer.upSync(path);
            syncStatusStore.setBackedupHash(path, localTriplet.getHash());
        } else {
            log.info("create remote directory for locally new directory: " + localFile.getAbsolutePath());
            try {
                syncer.createRemoteDir(path); // note that creating a remote directory does not ensure it is in sync
            } catch (ConflictException ex) {
                throw new IOException("Exception creating collection, probably already exists", ex);
            }
        }
    }

    @Override
    public void onLocalDeletion(Path path, ITriplet remoteTriplet) {
        final File localChild = toFile(path);
        log.info("Delete file from server for locally deleted file: " + localChild.getAbsolutePath());
        syncer.deleteRemote(path);
        syncStatusStore.clearBackedupHash(path);
    }

    @Override
    public void onTreeConflict(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException {
        Thread.dumpStack();
        final File localChild = toFile(path);
        Object[] options = {"Use my local file",
            "Use the remote file",
            "Do nothing"};
        String message = "Oh oh, remote is a " + typeOf(remoteTriplet) +" but local is a " + typeOf(localTriplet) +": " + localChild.getAbsolutePath();
        String title = "Tree conflict";
        int n = JOptionPane.showOptionDialog(null,
                message,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        if (n == JOptionPane.YES_OPTION) {
            onLocalDeletion(path, remoteTriplet);
            onLocalChange(localTriplet, path);
        } else if (n == JOptionPane.NO_OPTION) {
            onRemoteDelete(localTriplet, path);
            onRemoteChange(remoteTriplet, localTriplet, path);
        }
    }

    @Override
    public void onFileConflict(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException {
        final File localChild = toFile(path);        
        if( localChild.getParentFile().getName().equals(".mil")) {
            log.info("Conflict on .mil file, use remote: " + localChild.getAbsolutePath());
            onRemoteChange(remoteTriplet, localTriplet, path);
            return;
        }
        Thread.dumpStack();

        Object[] options = {"Use my local file",
            "Use the remote file",
            "Do nothing"};
        String message = "Files are in conflict. There has been a change to a local file, but also a change to the corresponding remote file: " + localChild.getAbsolutePath();
        String title = "File conflict";
        int n = JOptionPane.showOptionDialog(null,
                message,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        if (n == JOptionPane.YES_OPTION) {
            onLocalChange(localTriplet, path);
        } else if (n == JOptionPane.NO_OPTION) {
            onRemoteChange(remoteTriplet, localTriplet, path);
        }
    }

    private File toFile(Path path) {
        File f = root;
        for (String fname : path.getParts()) {
            f = new File(f, fname);
        }
        return f;
    }

    public boolean isReadonlyLocal() {
        return readonlyLocal;
    }

    public void setReadonlyLocal(boolean readonlyLocal) {
        this.readonlyLocal = readonlyLocal;
    }

    private String typeOf(ITriplet remoteTriplet) {
        if( remoteTriplet.getType().equals("d")) {
            return "directory";
        } else {
            return "file";
        }
    }
}
