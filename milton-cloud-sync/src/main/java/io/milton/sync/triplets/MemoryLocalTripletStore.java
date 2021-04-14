package io.milton.sync.triplets;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Parser;
import io.milton.event.EventManager;
import io.milton.sync.Syncer;
import io.milton.sync.Utils;
import io.milton.sync.event.EventUtils;
import io.milton.sync.event.FileChangedEvent;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.hashsplit4j.triplets.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryLocalTripletStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryLocalTripletStore.class);

    private final File root;
    private final FileSystemWatchingService fileSystemWatchingService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final EventManager eventManager;
    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final RepoChangedCallback callback;
    private final Consumer<Runnable> filter;
    private final List<String> ignorePatterns;

    private boolean initialScanDone;

    private final HashCalc hashCalc = HashCalc.getInstance();
    private boolean paused; // if true ignores fs events

    private final SyncHashCache fileHashCache;
    private long logTime;
    private String status;
    private List<WatchKey> watchKeys;

//    public MemoryLocalTripletStore(File root, BlobStore blobStore, HashStore hashStore) throws IOException {
//        this(root, null, blobStore, hashStore, null, null, null, null, null);
//    }

    public MemoryLocalTripletStore(File root, EventManager eventManager, BlobStore blobStore, HashStore hashStore, RepoChangedCallback callback,
            Consumer<Runnable> filter, FileSystemWatchingService fileSystemWatchingService, List<String> ignorePatterns, SyncHashCache fileHashCache) throws IOException {
        this.root = root;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.callback = callback;
        this.eventManager = eventManager;
        this.filter = filter;
        this.ignorePatterns = ignorePatterns;
        this.fileHashCache = fileHashCache;
        this.fileSystemWatchingService = fileSystemWatchingService;
        if( this.fileSystemWatchingService == null ) {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
        } else {
            this.scheduledExecutorService = fileSystemWatchingService.getScheduledExecutorService();
        }
    }


//    public MemoryLocalTripletStore(File root, EventManager eventManager, BlobStore blobStore, HashStore hashStore, RepoChangedCallback callback,
//            Consumer<Runnable> filter, File dataDir, FileSystemWatchingService fileSystemWatchingService, List<String> ignorePatterns) throws IOException {
//        this.root = root;
//        this.blobStore = blobStore;
//        this.hashStore = hashStore;
//        this.callback = callback;
//        this.eventManager = eventManager;
//        this.filter = filter;
//        this.ignorePatterns = ignorePatterns;
//
//        if (fileSystemWatchingService == null) {
//            this.fileSystemWatchingService = null;
//            scheduledExecutorService = Executors.newScheduledThreadPool(1);
////            final java.nio.file.Path path = FileSystems.getDefault().getPath(root.getAbsolutePath());
////            WatchService watchService = path.getFileSystem().newWatchService();
////            this.fileSystemWatchingService = new FileSystemWatchingService(watchService, scheduledExecutorService);
//            fileHashCache = new MemorySyncHashCache();
//        } else {
//            this.fileSystemWatchingService = fileSystemWatchingService;
//            this.scheduledExecutorService = fileSystemWatchingService.getScheduledExecutorService();
//            ICompositeCacheAttributes cfg = new CompositeCacheAttributes();
//            cfg.setUseDisk(true);
//            if (dataDir == null) {
//                dataDir = new File(System.getProperty("java.io.tmpdir"));
//            }
//            File envDir = new File(dataDir, "triplets");
//            log.trace("Create berkey db: " + envDir.getAbsolutePath());
//            envDir.mkdirs();
//            fileHashCache = new BerkeleyDbFileHashCache(envDir);
//        }
//    }

    public boolean isPaused() {
        return paused;
    }

    public String getStatus() {
        return status;
    }



    /**
     * Returns the new hash
     *
     * @return
     */
    public String scan() {

        log.info("START SCAN: " + root.getAbsolutePath());
        //Thread.dumpStack();
        String hash = null;
        try {
            hash = scanDirectory(root);
            if (callback != null) {
                callback.onChanged(hash);
            }
            if (eventManager != null) {
                this.status = "Firing event..";
                eventManager.fireEvent(new FileChangedEvent(root, hash));
            }

        } catch (Throwable e) {
            log.error("Exception in scan: " + root.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }

        if (!initialScanDone) {
            log.trace("Done initial scan");
            initialScanDone = true;
        }
        log.info("END SCAN");
        this.status = "";

        return hash;
    }

    /**
     * Start processing file system events
     *
     * @throws java.io.IOException
     */
    public void start() throws IOException {
        if (fileSystemWatchingService != null) {
            watchKeys = this.fileSystemWatchingService.watch(root, (WatchEvent.Kind<?> event, File changed) -> {
                processEvent(changed, event);
            });
            this.status = "start..";
            this.fileSystemWatchingService.start();
            this.status = "";
        }
    }

    public void stop() {
        this.initialScanDone = false;
        if( fileSystemWatchingService != null ) {
            this.fileSystemWatchingService.stop();
        }
        if( this.watchKeys != null ) {
            for( WatchKey key : watchKeys ) {
                key.cancel();
            }
        }
    }

    public String scanDirectory(File dir) throws IOException {
        if (Utils.ignored(dir, ignorePatterns)) {
            return null;
        }

        if (System.currentTimeMillis() - logTime > 2000) { // just output current dir every couple of seconds
            log.trace("scanDirectory: dir={}", dir.getAbsolutePath());
            logTime = System.currentTimeMillis();
        }


        System.out.print("/"); // progress indication
        this.status = "Scan directory " + dir.getAbsolutePath();
        //log.info("scanDirectory {}", dir);
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
        if (!blobStore.hasBlob(thisHash)) {
            blobStore.setBlob(thisHash, out.toByteArray());
        }

        // Need to store this in the blob store
        this.status = "";
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

        System.out.print("."); // just to give progress indication
        //log.info("scanFile: {}", f);
        String hash = (String) fileHashCache.get(f);
        if (hash != null) {
            return hash;
        }
        log.info("parsing: {}", f.getAbsolutePath());
        this.status = "Parsing file " + f.getAbsolutePath();

        hash = Parser.parse(f, blobStore, hashStore); // will generate blobs into this blob store

        if (blobStore instanceof BlockingBlobStore) {
            BlockingBlobStore bbs = (BlockingBlobStore) blobStore;
            try {
                //bbs.checkComplete();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        fileHashCache.put(f, hash);

        return hash;
    }

    private void processEvent(File f, WatchEvent.Kind<?> kind) {
        this.status = "process event for file " + f.getAbsolutePath();
        String changedPath = f.getAbsolutePath();
        if (changedPath.endsWith(".spliffy") || changedPath.endsWith(Syncer.TMP_SUFFIX)) {
            //ignore
        } else {
            if (paused) {
                log.trace("Ignoring fs events while paused during scan");
            } else {
                if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {

                    if (ignored(f) || ignored(f.getParentFile())) {
                        // ignore it
                    } else {
                        if (f.isDirectory()) {
                            directoryCreated(f);
                        } else {
                            fileCreated(f);
                        }
                    }
                } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                    if (ignored(f) || ignored(f.getParentFile())) {
                        log.trace("ignoring change to ignored file");
                    } else {
                        fileDeleted(f);
                    }
                } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                    if (ignored(f) || ignored(f.getParentFile())) {
                        // ignored
                    } else {
                        fileDeleted(f);
                    }
                } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                    if (ignored(f) || ignored(f.getParentFile())) {
                        log.trace("ignoring change to ignored file");
                    } else {
                        fileModified(f);
                    }
                }
            }
        }
        this.status = "";
    }

    private void directoryCreated(final File f) {
        log.info("Directory Created: " + f.getAbsolutePath());
    }

    private void fileCreated(File f) {
        log.info("file change detected: " + f.getAbsolutePath());
        scanDir(f.getParentFile());
    }

    private void fileModified(File f) {
        log.info("file change detected: " + f.getAbsolutePath());
        scanDir(f.getParentFile());
    }

    private void fileDeleted(File f) {
        log.info("file change detected: " + f.getAbsolutePath());
        scanDir(f.getParentFile());
    }

//    private final Set<File> scanningDirs = new HashSet<>();
//    private int queuedEvents;
//    private long lastEventTime;
    /**
     *
     * @param dir
     * @param deep - if true will scan the directory and its children, otherwise
     * only the directory
     */
//    private void scanDir(final File dir) {
//        lastEventTime = System.currentTimeMillis();
////        if (scanningDirs.contains(dir)) {
////            log.info("Not scanning directory {} because a scan is already queued or running for it", dir.getAbsoluteFile());
////            return;
////        }
//        scanningDirs.add(dir);
//        queuedEvents++;
//
//        scheduledExecutorService.schedule(() -> {
//            synchronized (MemoryLocalTripletStore.this) {
//                queuedEvents--;
//                if (queuedEvents < 0) {
//                    queuedEvents = 0;
//                }
//                try {
//                    if (filter != null) {
//                        filter.accept((Runnable) () -> {
//                            _scanDir(dir);
//                        });
//                    } else {
//                        _scanDir(dir);
//                    }
//                } catch (Throwable e) {
//                    log.error("An exception occurred scanning directory: " + dir.getAbsolutePath() + " because " + e.getMessage(), e);
//                    e.printStackTrace();
//                } finally {
//                    scanningDirs.remove(dir);
//                }
//            }
//        }, 500, TimeUnit.MILLISECONDS);
//    }
    private ScheduledFuture<?> future;
    private boolean isRunning;

    private void scanDir(final File dir) {
        if (future != null) {
            future.cancel(false);
        }
        future = scheduledExecutorService.schedule(() -> {
            _scanDir(dir);
        }, 1000, TimeUnit.MILLISECONDS);
    }

    private void _scanDir(final File dir) {
        try {
            isRunning = true;
            //log.info("scanDirTx: " + dir.getAbsolutePath());
            log.info("//*************** Start Scan - " + dir.getAbsolutePath() + "***************************");
            long tm = System.currentTimeMillis();

            String hash = scanDirectory(this.root);

            tm = System.currentTimeMillis() - tm;
            log.info("//*************** END Scan - " + dir.getName() + ", completed in " + tm + "ms " + "***************************");

            //log.info("fire FileChangedEvent event");
            if (callback != null) {
                callback.onChanged(hash);
            }
            EventUtils.fireQuietly(eventManager, new FileChangedEvent(this.root, hash));

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            isRunning = false;
        }

    }

    public File getRoot() {
        return root;
    }

    public boolean ignored(File childFile) {
        return Utils.ignored(childFile, ignorePatterns);
    }

    public interface RepoChangedCallback {

        public void onChanged(String newHash);
    }
}
