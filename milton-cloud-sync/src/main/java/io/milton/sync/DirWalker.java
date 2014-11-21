package io.milton.sync;

import io.milton.sync.triplets.TripletStore;
import io.milton.common.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Walks two directory structures, looking for differences, and invoking methods
 * on the given DeltaListener to resolve differences
 *
 * @author brad
 */
public class DirWalker {

    private static final Logger log = LoggerFactory.getLogger(DirWalker.class);
    private final TripletStore remoteTripletStore;
    private final TripletStore localTripletStore;
    private final SyncStatusStore syncStatusStore;
    private DeltaListener deltaListener;
    private final List<LocalDelete> localDeletes = new ArrayList<>();
    private boolean canceled = false;

    public DirWalker(TripletStore remoteTripletStore, TripletStore localTripletStore, SyncStatusStore syncStatusStore, DeltaListener deltaListener) {
        this.remoteTripletStore = remoteTripletStore;
        this.localTripletStore = localTripletStore;
        this.syncStatusStore = syncStatusStore;
        this.deltaListener = deltaListener;
    }

    public void walk() throws IOException {
//        log.info("DirWalker::walk ----------------------------");
        walk(Path.root());
        processLocalDeletes(); // we want to leave deletes until last in case there's some bytes we can use
//        log.info("DirWalker::End walk ----------------------------");
    }

    private void walk(Path path, String remoteDirHash, String localDirHash) throws IOException {
        log.info("walk1: " + path);
        List<ITriplet> remoteTriplets = findTriplets(path, remoteDirHash, remoteTripletStore);
        List<ITriplet> localTriplets = findTriplets(path, localDirHash, localTripletStore);
        walk(path, remoteTriplets, localTriplets, localDirHash, remoteDirHash);
    }

    private void walk(Path path) throws IOException {
        log.info("walk2: " + path);
        List<ITriplet> remoteTriplets = remoteTripletStore.getTriplets(path);
        List<ITriplet> localTriplets = localTripletStore.getTriplets(path);
        walk(path, remoteTriplets, localTriplets, null, null);
    }

    private void walk(Path path, List<ITriplet> remoteTriplets, List<ITriplet> localTriplets, String parentLocalHash, String parentRemoteHash) throws IOException {
        log.info("walk3: " + path);
        if (canceled) {
            log.trace("walk canceled");
            return;
        }
        Map<String, ITriplet> remoteMap = Utils.toMap(remoteTriplets);
        Map<String, ITriplet> localMap = Utils.toMap(localTriplets);

        //int numLocal = localTriplets == null ? 0 : localTriplets.size();
        //int numRemote = remoteTriplets == null ? 0 : remoteTriplets.size();
        //log.info("walk: " + path + " local items: " + numLocal + " - remote items: " + numRemote);        
        boolean didFindChange = false;

        if (remoteTriplets != null) {
            for (ITriplet remoteTriplet : remoteTriplets) {
                //System.out.println("remoteTriplet: " + remoteTriplet.getName());
                if (canceled) {
                    return;
                }
                Path childPath = path.child(remoteTriplet.getName());
                ITriplet localTriplet = localMap.get(remoteTriplet.getName());
                if (localTriplet == null) {
                    log.info("No localtriplet: " + remoteTriplet.getName() + " in folder: " + path);
                    doMissingLocal(remoteTriplet, childPath);
                    didFindChange = true;
                } else {
                    //log.info("Local {} Remote {}", localTriplet.getHash(), remoteTriplet.getHash());
                    if (localTriplet.getHash().equals(remoteTriplet.getHash())) {
                        // clean, nothing to do
                        //log.info("in sync: " + childPath);
                        //syncStatusStore.setBackedupHash(childPath, localTriplet.getHash());
                    } else {
                        log.info("different hashes: " + childPath + " local hash: " + localTriplet.getHash() + " remote hash: " + remoteTriplet.getHash());
                        doDifferentHashes(remoteTriplet, localTriplet, childPath);
                        didFindChange = true;
                    }
                }
            }
        }

        // Now look for local resources which do not match (by name) remote resources
        if (localTriplets != null) {
            for (ITriplet localTriplet : localTriplets) {
                if (!remoteMap.containsKey(localTriplet.getName())) {
                    log.info("Found missing remote");
                    Path childPath = path.child(localTriplet.getName());
                    doMissingRemote(localTriplet, childPath);
                    didFindChange = true;
                }
            }
        }
        if (didFindChange) {
            log.info("walk finished. Found and resolved changed: " + path);
        } else {
            log.warn("walk finished, did not find any changes- {} Local={} Remote={}", path, parentLocalHash, parentRemoteHash );
//            if (remoteTriplets != null) {
//                for (ITriplet remoteTriplet : remoteTriplets) {
//                    Path childPath = path.child(remoteTriplet.getName());
//                    ITriplet localTriplet = localMap.get(remoteTriplet.getName());
//                    log.info("{} {} {}", childPath.toString(), localTriplet.getHash(), remoteTriplet.getHash());
//                }
//                HashCalc c = new HashCalc();
//                ByteArrayOutputStream bout = new ByteArrayOutputStream();
//                c.sort(localTriplets);
//                String expectedLocal = c.calcHash(localTriplets, bout);
//                String updatedDirHash = localTripletStore.refreshDir(path);                
//                if( !updatedDirHash.equals(expectedLocal)) {
//                    throw new RuntimeException("Unexpected hash " + updatedDirHash + " - " + expectedLocal);
//                }
//                log.info("local expected hash={} updated dir hash={} ", expectedLocal, updatedDirHash);
//                System.out.println(bout.toString());
//            }
//            log.info("---- Done listing hashes");
            //Thread.dumpStack();
        }
    }

    /**
     * Called when there is a remote resource with no matching local resource
     *
     * Possibilities are: - it was in both local and remote, but has been
     * locally deleted - it is remotely new
     *
     * @param remoteTriplet
     * @param path
     */
    private void doMissingLocal(ITriplet remoteTriplet, Path path) throws IOException {
        String localPreviousHash = syncStatusStore.findBackedUpHash(path);
        if (localPreviousHash == null) {
            log.info("MISSING LOCAL1: " + path + "  no local backup hash, so remotely new");
            // not previously synced, so is remotely new
            deltaListener.onRemoteChange(remoteTriplet, remoteTriplet, path);
            if (Triplet.isDirectory(remoteTriplet)) {
                walk(path, remoteTriplet.getHash(), null);
            }
        } else {
            // was previously synced, now locally gone, so must have been deleted (or moved, same thing)
            log.info("MISSING LOCAL2: " + path + "  was previously backed up, so locally deleted");
            deltaListener.onLocalDeletion(path, remoteTriplet);
        }
    }

    /**
     * Called when there are local and remote resources with the same path, but
     * with different hashes
     *
     * Possibilities:
     *
     * both are directories: so just continue the scan
     *
     * both are files
     *
     * remote modified, local unchanged = downSync
     *
     * remote unchanged, local modified = upSync
     *
     * both changed = file conflict
     *
     * one is a file, the other a directory = tree conflict
     *
     * @param remoteTriplet
     * @param localTriplet
     * @param path
     */
    private void doDifferentHashes(ITriplet remoteTriplet, ITriplet localTriplet, Path path) throws IOException {
        if (Triplet.isDirectory(remoteTriplet) && Triplet.isDirectory(localTriplet)) {
            // both directories, so continue. Since we have the directory hashes we can lookup triplets on that instead of path
            walk(path, remoteTriplet.getHash(), localTriplet.getHash());
        } else if (!Triplet.isDirectory(remoteTriplet) && !Triplet.isDirectory(localTriplet)) {
            // both resources are files, check for consistency
            String localPreviousHash = syncStatusStore.findBackedUpHash(path);
            if (localPreviousHash == null) {
                // not previously synced, so is remotely new. But is different to server, so which is the latest? = conflict
                deltaListener.onFileConflict(remoteTriplet, localTriplet, path);
            } else {
                if (localPreviousHash.equals(localTriplet.getHash())) {
                    // local copy is unchanged from last sync, so we can safely down sync
                    deltaListener.onRemoteChange(remoteTriplet, localTriplet, path);
                } else {
                    if (localPreviousHash.equals(remoteTriplet.getHash())) {
                        // remote is identical to last synced, so no remote change. local has changed, so upload
                        deltaListener.onLocalChange(localTriplet, path);
                    } else {
                        System.out.println("---- File Conflict: " + path + " ----");
                        System.out.println("Local current hash: " + localTriplet.getHash());
                        System.out.println("Local last sync hash: " + localPreviousHash);
                        System.out.println("Remote current hash: " + remoteTriplet.getHash());
                        // local has changed from last sync, but server is different again. Clearly a CONFLICT
                        deltaListener.onFileConflict(remoteTriplet, localTriplet, path);
                    }
                }
            }
        } else {
            deltaListener.onTreeConflict(remoteTriplet, localTriplet, path);
        }
    }

    /**
     * Called when there is a local resource with no matching (by name) remote
     * resource
     *
     * Possibilities: - the resource has been added locally - if the resource is
     * a directory we continue scan - if a file we upSync it - the resource has
     * been remotely deleted
     *
     * @param localTriplet
     * @param childPath
     */
    private void doMissingRemote(ITriplet localTriplet, Path path) throws IOException {
        String localPreviousHash = syncStatusStore.findBackedUpHash(path);
        if (localPreviousHash == null) {
            // locally new
            deltaListener.onLocalChange(localTriplet, path);  // if resource is a directory this should create it            
            if (Triplet.isDirectory(localTriplet)) {  // continue scan
                List<ITriplet> localChildTriplets = findTriplets(path, localTriplet.getHash(), localTripletStore);
                walk(path, Collections.EMPTY_LIST, localChildTriplets, null, null);
            }
        } else {
            // it was previously synced, but now gone. So must have been deleted remotely            
            // So we want to "delete" the local resource. But its possible this is half
            // of a move operation, so instead of immediately deleting we will defer it
            log.info("Queueing local deletion: " + path + " because remote file is missing and there is a local sync record");
            LocalDelete localDelete = new LocalDelete(localTriplet, path);
            localDeletes.add(localDelete);
        }
    }

    private void processLocalDeletes() throws IOException {
        for (LocalDelete del : localDeletes) {
            deltaListener.onRemoteDelete(del.localTriplet, del.path);
        }
    }

    private List<ITriplet> findTriplets(Path path, String dirHash, TripletStore tripletStore) {
//        if( dirHash != null && tripletStore instanceof ParentHashAwareTripletStore) {
//            return  ((ParentHashAwareTripletStore)tripletStore).getTriplets(dirHash);
//        } else {
        return tripletStore.getTriplets(path);
//        }

    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * We want to defer local deletes until the end of the scan, because what
     * looks like a delete might actually be a move. By leaving the file in
     * place when we see a local add (downSync) we will have the bytes in place
     * to generate that file
     */
    class LocalDelete {

        final ITriplet localTriplet;
        final Path path;

        LocalDelete(ITriplet localTriplet, Path path) {
            this.localTriplet = localTriplet;
            this.path = path;
        }
    }
}
