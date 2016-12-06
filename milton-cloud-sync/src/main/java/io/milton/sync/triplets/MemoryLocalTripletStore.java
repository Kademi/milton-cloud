package io.milton.sync.triplets;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Parser;
import io.milton.event.EventManager;
import io.milton.sync.Syncer;
import io.milton.sync.Utils;
import io.milton.sync.event.EventUtils;
import io.milton.sync.event.FileChangedEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.CompositeCacheAttributes;
import org.apache.jcs.engine.behavior.ICompositeCacheAttributes;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLocalTripletStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryLocalTripletStore.class);
    private static final WatchEvent.Kind<?>[] events = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};

    private final File root;
    private final WatchService watchService;
    private final EventManager eventManager;
    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final RepoChangedCallback callback;
    private boolean initialScanDone;
    private ScheduledFuture<?> futureScan;
    private final ScheduledExecutorService scheduledExecutorService;
    private final HashCalc hashCalc = HashCalc.getInstance();
    private boolean paused; // if true ignores fs events

    private final JCS cache;


    /**
     *
     * @param root
     * @param eventManager
     * @param blobStore
     * @param hashStore
     * @param callback
     * @throws IOException
     */
    public MemoryLocalTripletStore(File root, EventManager eventManager, BlobStore blobStore, HashStore hashStore, RepoChangedCallback callback) throws IOException {
        this.root = root;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.callback = callback;
        this.eventManager = eventManager;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        final java.nio.file.Path path = FileSystems.getDefault().getPath(root.getAbsolutePath());
        watchService = path.getFileSystem().newWatchService();
        ICompositeCacheAttributes cfg = new CompositeCacheAttributes();
        cfg.setUseDisk(true);
        try {
            cache = JCS.getInstance("file-hashes", cfg);
        } catch (CacheException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void scan() {

        log.info("START SCAN");
        //Thread.dumpStack();
        try {
            String hash = scanDirectory(root);
            if( callback != null ) {
                callback.onChanged(hash);
            }
            eventManager.fireEvent(new FileChangedEvent(root, hash));

        } catch (Throwable e) {
            log.error("Exception in scan: " + root.getAbsolutePath(), e);
        }

        if (!initialScanDone) {
            log.info("Done initial scan");
            initialScanDone = true;
        }
        log.info("END SCAN");
    }

    /**
     * Start processing file system events
     */
    public void start() {
        // after initial scan is done, start the thread which will process file changed events
        // note these events begin accumulating as the scan processes directories
        Runnable rScan = () -> {
            try {
                scanFsEvents();
            } catch (IOException ex) {
                log.error("Exception processing events", ex);
            }
        };
        log.info("Begin file watch loop: " + root.getAbsolutePath());
        futureScan = scheduledExecutorService.scheduleWithFixedDelay(rScan, 200, 200, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop processing file system events
     */
    public void stop() {
        if (futureScan != null) {
            futureScan.cancel(true);
        }
    }



    public String scanDirectory(File dir) throws IOException {
        if (Utils.ignored(dir)) {
            return null;
        }
        if (!initialScanDone) {
            registerWatchDir(dir);
        }
        log.info("scanDirectory: dir={}", dir.getAbsolutePath());

        File[] children = dir.listFiles();

        List<ITriplet> triplets = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                Triplet t = new Triplet();
                t.setName(child.getName());
                if (child.isDirectory()) {
                    t.setType("d");
                    String dirHash = scanDirectory(child);
                    if (dirHash == null) {
                        t = null;
                    } else {
                        t.setHash(dirHash);
                    }
                } else {
                    t.setType("f");
                    String fileHash = scanFile(child);
                    if (fileHash == null) {
                        t = null;
                    } else {
                        t.setHash(fileHash);
                    }
                }
                if (t != null) {
                    triplets.add(t);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String thisHash = hashCalc.calcHash(triplets, out);
        blobStore.setBlob(thisHash, out.toByteArray());

        // Need to store this in the blob store


        return thisHash;

    }

    /**
     * Scan the file and generate records
     *
     * @param c
     * @param f
     */
    private String scanFile(File f) throws IOException {
        if (f.isDirectory()) {
            return null; // will generate directory records in scan after all children are processed
        }

        String fileKey = genFileKey(f);
        String hash = (String) cache.get(fileKey);
        if (hash != null) {
            return hash;
        }

        hash = Parser.parse(f, blobStore, hashStore); // will generate blobs into this blob store
        try {
            cache.put(fileKey, hash);
        } catch (CacheException ex) {
            log.warn("Could not add to cache", ex);
        }

        return hash;
    }

    private final Map<File, WatchKey> mapOfWatchKeysByDir = new HashMap<>();

    private void registerWatchDir(final File dir) throws IOException {
        if (mapOfWatchKeysByDir.containsKey(dir)) {
            WatchKey key = mapOfWatchKeysByDir.get(dir);
            key.cancel();
            mapOfWatchKeysByDir.remove(dir);
        }
        final java.nio.file.Path path = FileSystems.getDefault().getPath(dir.getAbsolutePath());
        // will only watch specified directory, not subdirectories
        WatchKey key = path.register(watchService, events);
        mapOfWatchKeysByDir.put(dir, key);
        log.info("Now watching: " + dir.getAbsolutePath());
    }

    private void unregisterWatchDir(final File dir) {
        final java.nio.file.Path path = FileSystems.getDefault().getPath(dir.getAbsolutePath());
        WatchKey key = mapOfWatchKeysByDir.get(dir);
        if (key != null) {
            log.info("Cancel watch: " + dir.getAbsolutePath() + " - " + key.watchable());
            key.cancel();
        }

    }

    private int queuedEvents;
    private long lastEventTime;

    private void scanFsEvents() throws IOException {
        WatchKey watchKey;
        watchKey = watchService.poll(); // this call is blocking until events are present
        if (watchKey == null) {
            return;
        }

        Watchable w = watchKey.watchable();
        java.nio.file.Path watchedPath = (java.nio.file.Path) w;
        // poll for file system events on the WatchKey

        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            // log.info("scanFsEvents: " + System.currentTimeMillis());
            WatchEvent.Kind<?> kind = event.kind();
            java.nio.file.Path p = (java.nio.file.Path) event.context();
            if (p.toString().endsWith(".spliffy") || p.toString().endsWith(Syncer.TMP_SUFFIX)) {
                //ignore
            } else {
                if (paused) {
                    log.info("Ignoring fs events while paused during scan");
                } else {
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        java.nio.file.Path pathCreated = (java.nio.file.Path) event.context();

                        final File f = new File(watchedPath + File.separator + pathCreated);
                        if (Utils.ignored(f) || Utils.ignored(f.getParentFile())) {
                            // ignore it
                        } else {
                            log.info("scanFsEvents: watchedPath=" + watchedPath);
                            if (f.isDirectory()) {
                                directoryCreated(f);
                            } else {
                                fileCreated(f);
                            }
                        }
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        java.nio.file.Path pathDeleted = (java.nio.file.Path) event.context();
                        final File f = new File(watchedPath + File.separator + pathDeleted);
                        if (Utils.ignored(f) || Utils.ignored(f.getParentFile())) {
                            log.info("ignoring change to ignored file");
                        } else {
                            fileDeleted(f);
                        }
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        java.nio.file.Path pathDeleted = (java.nio.file.Path) event.context();
                        final File f = new File(watchedPath + File.separator + pathDeleted);
                        if (Utils.ignored(f) || Utils.ignored(f.getParentFile())) {
                            // ignored
                        } else {
                            fileDeleted(f);
                        }
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        java.nio.file.Path pathModified = (java.nio.file.Path) event.context();
                        final File f = new File(watchedPath + File.separator + pathModified);
                        if (Utils.ignored(f) || Utils.ignored(f.getParentFile())) {
                            log.info("ignoring change to ignored file");
                        } else {
                            fileModified(f);
                        }
                    }
                }
            }
        }

        // if the watched directed gets deleted, get out of run method
        if (!watchKey.reset()) {
            log.info("Watch is no longer valid");
            watchKey.cancel();
        }
    }

    private void directoryCreated(final File f) {
        log.info("Directory Created: " + f.getAbsolutePath());
        try {
            registerWatchDir(f);
            scanDir(f.getParentFile(), false); // scan the parent, so it can see the new member
        } catch (IOException e) {
            log.error("Exception in directoryCreated", e);
        }
    }

    private void fileCreated(File f) {
        log.info("fileCreated: " + f.getAbsolutePath());
        scanDir(f.getParentFile(), false);
    }

    private void fileModified(File f) {
        log.info("fileModified: " + f.getAbsolutePath());
        scanDir(f.getParentFile(), false);
    }

    private void fileDeleted(File f) {
        log.info("file deleted " + f.getAbsolutePath());
        unregisterWatchDir(f); // f might be a file or directory, but unregister checks for presence so ok to call regardless
        scanDir(f.getParentFile(), false);
    }

    private final Set<File> scanningDirs = new HashSet<>();

    /**
     *
     * @param dir
     * @param deep - if true will scan the directory and its children, otherwise
     * only the directory
     */
    private void scanDir(final File dir, final boolean deep) {
        if (scanningDirs.contains(dir)) {
            log.info("Not scanning directory {} because a scan is already queued or running for it", dir.getAbsoluteFile());
            return;
        }
        scanningDirs.add(dir);
        queuedEvents++;
        lastEventTime = System.currentTimeMillis();
        scheduledExecutorService.schedule(() -> {
            synchronized (MemoryLocalTripletStore.this) {
                queuedEvents--;
                if (queuedEvents < 0) {
                    queuedEvents = 0;
                }
                try {
                    _scanDir(dir, deep);
                } catch (Throwable e) {
                    log.error("An exception occurred scanning directory: " + dir.getAbsolutePath() + " because " + e.getMessage(), e);
                } finally {
                    scanningDirs.remove(dir);
                }
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void _scanDir(final File dir, final boolean deep) throws IOException {
        log.info("scanDirTx: " + dir.getAbsolutePath());
        log.info("//*************** Start Scan - " + dir.getName() + "***************************");
        String hash = scanDirectory(this.root);

        log.info("//*************** END Scan - " + dir.getName() + "***************************");

        long durationSinceLastEvent = System.currentTimeMillis() - lastEventTime;
        log.info("finished scan dir queuedEvents={} duration since last event={} ms", queuedEvents, durationSinceLastEvent);
        if (queuedEvents < 0) {
            log.warn("huh?? queuedEvents={}", queuedEvents);
        }
        if (queuedEvents <= 0 || durationSinceLastEvent > 5000) {
            queuedEvents = 0;
            log.info("No more queued events, or its been a while, so fire FileChangedEvent event");
            if( callback != null ) {
                callback.onChanged(hash);
            }
            EventUtils.fireQuietly(eventManager, new FileChangedEvent(this.root, hash));
        } else {
            log.info("Not firing file changed event because queued events is not empty queuedEvents={} duration since last event={} ms", queuedEvents, durationSinceLastEvent);
        }

    }

    public File getRoot() {
        return root;
    }

    private String genFileKey(File f) {
        return f.getAbsolutePath() + "-" + f.lastModified();
    }


    public interface RepoChangedCallback {
        public void onChanged(String newHash);
    }
}
