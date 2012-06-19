package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.ConflictException;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import io.milton.cloud.common.Triplet;
import io.milton.sync.triplets.JdbcLocalTripletStore;

/**
 *
 * @author brad
 */
class SyncingDeltaListener implements DeltaListener2 {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SyncingDeltaListener.class);
    private final Syncer syncer;
    private final Archiver archiver;
    private final File root;
    private final SyncStatusStore syncStatusStore;

    public SyncingDeltaListener(Syncer syncer, Archiver archiver, File localRoot, SyncStatusStore syncStatusStore) {
        this.syncer = syncer;
        this.archiver = archiver;
        this.root = localRoot;
        this.syncStatusStore = syncStatusStore;
    }

        
    @Override
    public void onRemoteChange(Triplet remoteTriplet, Triplet localTriplet, Path path) throws IOException {        
        if( remoteTriplet.isDirectory()) {
            final File localFile = toFile(path);
            if( !localFile.exists() ) {
                log.info("new remote directory, create: " + localFile.getAbsolutePath());
                if( !localFile.mkdirs() ) {
                    throw new IOException("Couldnt create local directory: " + localFile.getAbsolutePath());
                }
            }
        } else {
            final File localChild = toFile(path);
            log.info("new or modified remote file: " + localChild.getAbsolutePath());
            syncer.downloadSync(remoteTriplet.getHash(), path);
            syncStatusStore.setBackedupHash(path, remoteTriplet.getHash());
        }        
    }
    
    @Override
    public void onRemoteDelete(Triplet localTriplet, Path path) {
        final File localChild = toFile(path);
        log.info("Archiving remotely deleted file: " + localChild.getAbsolutePath());        
        archiver.archive(localChild);
        syncStatusStore.clearBackedupHash(path);
    }    

    @Override
    public void onLocalChange(Triplet localTriplet, Path path) throws IOException {
        final File localFile = toFile(path);
        if (localFile.isFile()) {
            System.out.println("upload locally new or modified file: " + localFile.getAbsolutePath());
            syncer.upSync(path);
            syncStatusStore.setBackedupHash(path, localTriplet.getHash());
        } else {
            System.out.println("create remote directory for locally new directory: " + localFile.getAbsolutePath());
            try {
                syncer.createRemoteDir(path); // note that creating a remote directory does not ensure it is in sync
            } catch (ConflictException ex) {
                throw new IOException("Exception creating collection, probably already exists", ex);
            }
        }
    }
    
    @Override
    public void onLocalDeletion(Path path, Triplet remoteTriplet) {
        final File localChild = toFile(path);
        System.out.println("Delete file from server for locally deleted file: " + localChild.getAbsolutePath());
        syncer.deleteRemote(path);
        syncStatusStore.clearBackedupHash(path);
    }    

    @Override
    public void onTreeConflict(Triplet remoteTriplet, Triplet localTriplet, Path path) {
        Thread.dumpStack();
        final File localChild = toFile(path);
        JOptionPane.showMessageDialog(null, "Oh oh, remote is a file but local is a directory: " + localChild.getAbsolutePath());
    }

    @Override
    public void onFileConflict(Triplet remoteTriplet, Triplet localTriplet, Path path) {
        Thread.dumpStack();
        final File localChild = toFile(path);
        JOptionPane.showMessageDialog(null, "Files are in conflict. There has been a change to a local file, but also a change to the corresponding remote file: " + localChild.getAbsolutePath());
    }



    private File toFile(Path path) {
        File f = root;
        for (String fname : path.getParts()) {
            f = new File(f, fname);
        }
        return f;
    }    
}
