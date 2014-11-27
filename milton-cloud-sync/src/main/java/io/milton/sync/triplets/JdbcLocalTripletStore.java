package io.milton.sync.triplets;

import com.bradmcevoy.utils.With;
import io.milton.common.Path;
import com.ettrema.db.Table;
import com.ettrema.db.TableCreatorService;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.NullHashStore;
import org.hashsplit4j.api.Parser;
import io.milton.event.EventManager;
import io.milton.sync.Syncer;
import io.milton.sync.Utils;
import io.milton.sync.event.EventUtils;
import io.milton.sync.event.FileChangedEvent;
import io.milton.sync.triplets.BlobDao.BlobVector;
import io.milton.sync.triplets.CrcDao.CrcRecord;
import java.util.HashMap;
import org.hashsplit4j.triplets.HashCalc;
import org.hashsplit4j.triplets.ITriplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the given root directory on startup to ensure the triplet table is up
 * to date.
 *
 * Once scanning is complete it acts as a TripletStore
 *
 * Does a depth-first scan of the directory. For each file it checks if the file
 * is present in the table and if the modified date is unchanged
 *
 * If anything was changed after scanning the children of a directory then the
 * directories own hash is updated (which is why it must be depth first)
 *
 * As part of scanning files this will populate a blobs table containing the
 * hash , offset and length of the blob, allowing this to be used as a blob
 * source
 *
 * @author brad
 */
public class JdbcLocalTripletStore implements TripletStore, BlobStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcLocalTripletStore.class);
    private static final ThreadLocal<Connection> tlConnection = new ThreadLocal<>();
    private static final WatchEvent.Kind<?>[] events = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};

    private static Connection con() {
        return tlConnection.get();
    }
    private final UseConnection useConnection;
    private final CrcDao crcDao;
    private final BlobDao blobDao;
    private final File root;
    private final WatchService watchService;
    private final EventManager eventManager;
    private File currentScanFile;
    private long currentOffset;
    private String lastBlobHash;
    private byte[] lastBlob;
    private boolean initialScanDone;
    private ScheduledFuture<?> futureScan;
    private final ScheduledExecutorService scheduledExecutorService;
    private final HashCalc hashCalc = HashCalc.getInstance();

    /**
     *
     * @param useConnection
     * @param dialect
     * @param group - so we can cache different collections in one table
     */
    public JdbcLocalTripletStore(UseConnection useConnection, Dialect dialect, File root, EventManager eventManager) throws IOException {
        this.useConnection = useConnection;
        this.root = root;
        this.eventManager = eventManager;
        this.crcDao = new CrcDao();
        this.blobDao = new BlobDao();
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        final java.nio.file.Path path = FileSystems.getDefault().getPath(root.getAbsolutePath());
        watchService = path.getFileSystem().newWatchService();

        TableDefinitionSource defs = new TableDefinitionSource() {

            @Override
            public List<? extends Table> getTableDefinitions() {
                return Arrays.asList(CrcDao.CRC, BlobDao.BLOB);
            }

            @Override
            public void onCreate(Table t, Connection con) {
            }
        };
        final TableCreatorService creatorService = new TableCreatorService(null, Arrays.asList(defs), dialect);

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                creatorService.processTableDefinitions(con);
                return null;
            }
        });
    }

    @Override
    public List<ITriplet> getTriplets(Path path) {
        try {
            if (!initialScanDone) {
                log.info("getTriplets: Initial scan not done, doing it now...");
                //Thread.dumpStack();
                scan();
                initialScanDone = true;
                log.info("getTriplets: Initial scan finished. Now, proceed with syncronisation...");
            }

            final File f = Utils.toFile(root, path);
            List<CrcRecord> records = useConnection.use(new With<Connection, List<CrcRecord>>() {

                @Override
                public List<CrcRecord> use(Connection con) throws Exception {
                    tlConnection.set(con);
                    List<CrcRecord> list = crcDao.listCrcRecords(con, f.getAbsolutePath());
                    long count = crcDao.getCrcRecordCount(con());
                    tlConnection.remove();
                    return list;
                }
            });
            // log.trace("JdbcLocalTripletStore: getTriplets: " + f.getAbsolutePath() + " - " + records.size());
            List<ITriplet> triplets = BlobUtils.toTriplets(f, records);
            HashCalc c = new HashCalc();
            String expectedLocal = c.calcHash(triplets);

            return triplets;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String refreshDir(Path path) {
        final File f = Utils.toFile(root, path);
        String newHash = useConnection.use(new With<Connection, String>() {

            @Override
            public String use(Connection con) throws Exception {
                tlConnection.set(con);
                String newHash = generateDirectoryRecordRecusive(con, f);
                tlConnection.remove();
                return newHash;
            }
        });
        return newHash;
    }

    /**
     * Must be inside a connection
     *
     * @param hash
     * @param bytes
     */
    @Override
    public void setBlob(String hash, byte[] bytes) {
        blobDao.insertBlob(hash, bytes, currentScanFile.getAbsolutePath(), currentOffset, con());
        currentOffset += bytes.length;
    }

    @Override
    public byte[] getBlob(String hash) {
        try {
            if (hash.equals(lastBlobHash)) {  // this will often happen because hasBlob will be called first for same hash
                return lastBlob;
            }
            List<BlobVector> list = blobDao.listBlobsByHash(con(), hash);

            for (BlobVector v : list) {
                try {
                    byte[] blob = BlobUtils.loadAndVerify(currentScanFile, v);
                    if (blob != null) {
                        lastBlobHash = hash;
                        lastBlob = blob;
                    }
                    return blob;
                } catch (IOException e) {
                    System.out.println("couldnt load from vector: " + v + "  probably no longer valid so will delete the blob record");
                    blobDao.deleteBlob(v.path, v.crc, v.offset, con());
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasBlob(String hash) {
        return getBlob(hash) != null;
    }

    public void scan() {
        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection t) throws Exception {
                log.info("START SCAN");
                //Thread.dumpStack();
                try {
                    tlConnection.set(t);
                    scanDirectory(root);
                    con().commit();

                    long count = crcDao.getCrcRecordCount(con());
                    //log.info("scan: Contains crc records: " + count);

                } catch (Throwable e) {
                    log.error("Exception in scan: " + root.getAbsolutePath(), e);
                } finally {
                    tlConnection.remove();
                }

                return null;
            }
        });
        if (!initialScanDone) {
            log.info("Done initial scan");
            initialScanDone = true;
        }

    }

    /**
     * Start processing file system events
     */
    public void start() {
        // after initial scan is done, start the thread which will process file changed events
        // note these events begin accumulating as the scan processes directories
        Runnable rScan = new Runnable() {

            @Override
            public void run() {
                try {
                    scanFsEvents();
                } catch (IOException ex) {
                    log.error("Exception processing events", ex);
                }
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

    /**
     *
     * @param dir
     * @return - true if anything was changed
     */
    private boolean scanDirectory(File dir) throws SQLException, IOException {
        List<CrcRecord> oldRecords = crcDao.listCrcRecords(con(), dir.getParentFile().getAbsolutePath());
        Map<String, CrcRecord> mapOfRecords = CrcDao.toMap(oldRecords);
        CrcRecord rec = mapOfRecords.get(dir.getName());
        String oldHash = null;
        if( rec != null ) {
            oldHash = rec.crc;
        }
        return scanDirectory(dir, oldHash);
    }
    
    /**
     * 
     * @param dir
     * @param dirHash - previous hash, null if the directory has not been seen before
     * @return
     * @throws SQLException
     * @throws IOException 
     */
    private boolean scanDirectory(File dir, String dirHash) throws SQLException, IOException {
        if (Utils.ignored(dir)) {
            return false;
        }
        if (!initialScanDone) {
            registerWatchDir(dir);
        }
        log.info("scanDirectory: dir={} old hash={}", dir.getAbsolutePath(), dirHash);
        
        final Map<String, File> mapOfFiles = Utils.toMap(dir.listFiles());
        
        List<CrcRecord> oldRecords = crcDao.listCrcRecords(con(), dir.getAbsolutePath());
        Map<String, CrcRecord> mapOfRecords = CrcDao.toMap(oldRecords);
        
        
        File[] children = dir.listFiles();
        boolean changed = (dirHash == null); // if no previous has then definitely changed
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    String oldChildHash = null;
                    CrcRecord rec = mapOfRecords.get(child.getName());
                    if( rec != null ) {
                        oldChildHash = rec.crc;
                    }
                    if (scanDirectory(child, oldChildHash)) {
                        changed = true;
                    }
                }
                if( !mapOfRecords.containsKey(child.getName())) {
                    log.info("A resource has been added: " + child.getName());
                    changed = true;
                }
            }
        }
        
        if (scanChildren(dir, mapOfFiles, mapOfRecords)) {
            changed = true;
        }

        if (changed) {
            log.info("changed records found, refresh diretory record: " + dir.getAbsolutePath());
            generateDirectoryRecordRecusive(con(), dir); // insert/update the hash for this directory
        }

        con().commit(); // commit every now and then
        return changed;
    }

    /**
     * Get all the records for this directory and compare them with current
     * files. Delete records with no corresponding file/directory, insert new
     * ones, and update if the recorded modified date differs from actual
     *
     * @param dir
     * @return - true if anything has changed
     */
    private boolean scanChildren(final File dir, final Map<String, File> mapOfFiles, Map<String, CrcRecord> mapOfRecords) throws SQLException, IOException {
        log.info("scanChildren: dir={}", dir.getAbsolutePath());        

        boolean changed = false;

        // remove any that no longer exist
        for (CrcRecord r : mapOfRecords.values()) {
            //log.info("scanChildren check: {}", r.name);
            if (!mapOfFiles.containsKey(r.name)) {
                changed = Boolean.TRUE;
                File fRemoved = new File(dir, r.name);
                log.info("detected change, file removed: " + fRemoved.getAbsolutePath());
                crcDao.deleteCrc(con(), dir.getAbsolutePath(), r.name);
            }
        }

        for (File f : mapOfFiles.values()) {
            if (f.isFile()) {
                if (!f.getName().endsWith(Syncer.TMP_SUFFIX)) {
                    CrcRecord r = mapOfRecords.get(f.getName());
                    if (r == null) {
                        log.info("detected change, new file: " + f.getAbsolutePath() + " in map of size: " + mapOfRecords.size());
                        changed = Boolean.TRUE;
                        scanFile(con(), f);
                    } else {
                        if (r.date.getTime() != f.lastModified()) {
                            log.info("detected change, file modified dates differ: " + f.getAbsolutePath());
                            changed = Boolean.TRUE;
                            crcDao.deleteCrc(con(), dir.getAbsolutePath(), f.getName());
                            scanFile(con(), f);
                        } else {
                            //log.trace("scanChildren: file is up to date: " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Scan the file and generate records
     *
     * @param c
     * @param f
     */
    private void scanFile(Connection c, File f) throws IOException, SQLException {
        if (f.isDirectory()) {
            return; // will generate directory records in scan after all children are processed
        }
        this.currentScanFile = f; // will be used by setBlob
        this.currentOffset = 0;
        String crc = Parser.parse(f, this, new NullHashStore()); // will generate blobs into this blob store
        this.currentScanFile = null;
        crcDao.insertCrc(c, f.getParentFile().getAbsolutePath(), f.getName(), crc, f.lastModified());
    }

    private String generateDirectoryRecordRecusive(Connection con, File f) throws SQLException, IOException {
        String newHash = generateDirectoryRecord(con, f);
        File parent = f;
        while (!parent.equals(root)) {
            //log.info("generateDirectoryRecordRecusive - " + parent.getAbsolutePath() + " != " + root.getAbsolutePath());
            parent = parent.getParentFile();            
            generateDirectoryRecord(con, parent);
        }
        return newHash;
    }

    /**
     * Called after all children of the directory have been processed, and only
     * if a change was detected in a child
     *
     * @param dir
     */
    private String generateDirectoryRecord(Connection c, File dir) throws SQLException, IOException {
        //log.info("generateDirectoryRecord: {}", dir.getAbsolutePath());
        crcDao.deleteCrc(con(), dir.getParent(), dir.getName());
        // Note that we're reloading triplets, strictly not necessary but is a bit safer then
        // reusing the list we've been changing

        List<CrcRecord> crcRecords = crcDao.listCrcRecords(con(), dir.getAbsolutePath());
        List<ITriplet> triplets = BlobUtils.toTriplets(dir, crcRecords);
        hashCalc.sort(triplets);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String newHash = hashCalc.calcHash(triplets, bout);
        log.info("Insert new directory hash: " + dir.getParent() + " :: " + dir.getName() + " = " + newHash);
        //log.info(bout.toString());
        crcDao.insertCrc(c, dir.getParentFile().getAbsolutePath(), dir.getName(), newHash, dir.lastModified());
        return newHash;
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
            log.info("scanFsEvents: " + System.currentTimeMillis());
            WatchEvent.Kind<?> kind = event.kind();
            java.nio.file.Path p = (java.nio.file.Path) event.context();
            if (p.toString().endsWith(".spliffy") || p.toString().endsWith(Syncer.TMP_SUFFIX)) {
                //ignore
            } else {
                if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                    java.nio.file.Path pathCreated = (java.nio.file.Path) event.context();

                    final File f = new File(watchedPath + File.separator + pathCreated);
                    log.info("scanFsEvents: watchedPath=" + watchedPath);
                    if (f.isDirectory()) {
                        scheduledExecutorService.schedule(new Runnable() {

                            @Override
                            public void run() {
                                directoryCreated(f);
                            }
                        }, 500, TimeUnit.MILLISECONDS);
                    } else {
                        scheduledExecutorService.schedule(new Runnable() {

                            @Override
                            public void run() {
                                fileCreated(f);
                            }
                        }, 500, TimeUnit.MILLISECONDS);

                    }
                } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                    java.nio.file.Path pathDeleted = (java.nio.file.Path) event.context();
                    final File f = new File(watchedPath + File.separator + pathDeleted);

                    scheduledExecutorService.schedule(new Runnable() {

                        @Override
                        public void run() {
                            fileDeleted(f);
                        }
                    }, 500, TimeUnit.MILLISECONDS);

                } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                    java.nio.file.Path pathModified = (java.nio.file.Path) event.context();
                    final File f = new File(watchedPath + File.separator + pathModified);
                    scheduledExecutorService.schedule(new Runnable() {

                        @Override
                        public void run() {
                            fileModified(f);
                        }
                    }, 500, TimeUnit.MILLISECONDS);

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
            scanDirTx(f.getParentFile()); // scan the parent, so it can see the new member
        } catch (IOException e) {
            log.error("Exception in directoryCreated", e);
        }
    }

    private void fileCreated(File f) {
        log.info("fileCreated: " + f.getAbsolutePath());
        scanDirTx(f.getParentFile());
    }

    private void fileModified(File f) {
        log.info("fileModified: " + f.getAbsolutePath());
        scanDirTx(f.getParentFile());
    }

    private void fileDeleted(File f) {
        log.info("file deleted " + f.getAbsolutePath());
        unregisterWatchDir(f); // f might be a file or directory, but unregister checks for presence so ok to call regardless
        scanDirTx(f.getParentFile());
    }

    private void scanDirTx(final File dir) {
        log.info("scanDirTx: " + dir.getAbsolutePath());
        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection t) throws Exception {
                tlConnection.set(t);
                if (scanDirectory(dir)) {
                    log.info("scanDirectory says something changed");
                } else {
                    log.info("scanDirectory says nothing changed");
                }
                con().commit();

                log.info("something probably changed, fire event so everyone knows");
                EventUtils.fireQuietly(eventManager, new FileChangedEvent());

                tlConnection.remove();
                return null;
            }
        });
    }

}
