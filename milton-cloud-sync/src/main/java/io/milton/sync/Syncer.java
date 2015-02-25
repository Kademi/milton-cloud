package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.*;
import io.milton.event.EventManager;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.HttpResult;
import io.milton.httpclient.MethodNotAllowedException;
import io.milton.httpclient.NotifyingFileInputStream;
import io.milton.httpclient.ProgressListener;
import io.milton.sync.event.DownloadSyncEvent;
import io.milton.sync.event.EventUtils;
import io.milton.sync.event.FinishedSyncEvent;
import io.milton.sync.event.TransferProgressEvent;
import io.milton.sync.event.UploadSyncEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.hashsplit4j.store.ByteArrayBlobStore;
import org.hashsplit4j.triplets.HashCalc;

/**
 * This class contains the code to actually perform file sync operations. This
 * is generally called from SyncingDeltaListener in response to file comparison
 * events.
 *
 * @author brad
 */
public class Syncer {
    
   
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Syncer.class);
    
    public static final String TMP_SUFFIX = ".new.tmp";
    
    private final EventManager eventManager;
    private final HttpHashStore httpHashStore;
    private final HttpBlobStore httpBlobStore;
    private final Host host;
    private final Archiver archiver;
    private final File root;
    private final Path baseUrl;
    private final HashCalc hashCalc = HashCalc.getInstance();
    private boolean paused;
    private boolean readonlyLocal;
    private Combiner combiner;
    private Parser parser;
    private ProgressListener progressListener = new EventProgressListener();

    public Syncer(EventManager eventManager, File root, HttpHashStore httpHashStore, HttpBlobStore httpBlobStore, Host host, Archiver archiver, String baseUrl) {
        this.eventManager = eventManager;
        this.root = root;
        this.httpHashStore = httpHashStore;
        this.httpBlobStore = httpBlobStore;
        this.archiver = archiver;
        this.host = host;
        this.baseUrl = Path.path(baseUrl);
    }

    public void createRemoteDir(Path path) throws ConflictException {
        if (paused) {
            return;
        }
        Path p = baseUrl.add(path);
        try {
            File localFile = toFile(path);
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent(localFile));
            host.doMkCol(p);
        } catch (MethodNotAllowedException e) {
            log.warn("Tried to create a remote folder, but got a conflict which probably means it already exists so just carry on: " + e.getMessage());                    
            // this should mean that the folder already exists, so cool            
            //throw new ConflictException(p.toString());
        } catch (HttpException ex) {
            throw new RuntimeException(ex);
        } catch (NotAuthorizedException ex) {
            throw new RuntimeException(ex);
        } catch (BadRequestException ex) {
            throw new RuntimeException(ex);
        } catch (NotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
        }
    }

    public void deleteRemote(Path path) {
        if (paused) {
            return;
        }

        Path p = baseUrl.add(path);
        try {
            File localFile = toFile(path);
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent(localFile));
            host.doDelete(p);
        } catch (NotFoundException e) {
            // ok
        } catch (IOException | HttpException | NotAuthorizedException | ConflictException | BadRequestException ex) {
            Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
        }
    }

    public void downloadSync(String hash, Path path) throws IOException {
        if (paused) {
            return;
        }

        if (readonlyLocal) {
            log.warn("Not downsyncing because local is readonly");
            return;
        }
        try {
            File localFile = toFile(path);
            EventUtils.fireQuietly(eventManager, new DownloadSyncEvent(localFile));
            _downloadSync(hash, path, localFile);
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
        }
    }

    private void _downloadSync(String fileHash, Path path, File localFile) throws IOException {
        log.info("downloadSync: " + path);
        List<HashStore> hashStores = new ArrayList<>();
        List<BlobStore> blobStores = new ArrayList<>();

        // If we have a temp file it means a previous download didnt complete. Lets
        // use it as a store so we can resume the download
        File fTemp = new File(localFile.getAbsolutePath() + TMP_SUFFIX);
        FileBlobStore partialDownloadBlobStore = null;
        FileBlobStore oldFileBlobStore = null;
        try {
            if (fTemp.exists()) {
                if (fTemp.isFile()) {
                    // found previous download file, so use it as a hashstore
                    MemoryHashStore partialDownloadHashStore = new MemoryHashStore();
                    partialDownloadBlobStore = new FileBlobStore(fTemp);
                    partialDownloadBlobStore.openForRead();
                    Parser.parse(fTemp, partialDownloadBlobStore, partialDownloadHashStore);
                    hashStores.add(partialDownloadHashStore);
                    blobStores.add(partialDownloadBlobStore);
                } else {
                    FileUtils.deleteDirectory(fTemp);
                }
            }

            // Also use the current file (if it exists!) as a hash and blob store, since we're hoping
            // much of the file is unchanged
            if (localFile.exists()) {
                MemoryHashStore oldFileHashStore = new MemoryHashStore();
                oldFileBlobStore = new FileBlobStore(localFile);
                oldFileBlobStore.openForRead();
                Parser.parse(localFile, oldFileBlobStore, oldFileHashStore);
                hashStores.add(oldFileHashStore);
                blobStores.add(oldFileBlobStore);
            }

            // Now add the remote stores, where we will download anything not present locally
            hashStores.add(httpHashStore);
            blobStores.add(httpBlobStore);

            HashStore multiHashStore = new MultipleHashStore(hashStores);
            BlobStore multiBlobStore = new MultipleBlobStore(blobStores);

            Fanout rootRemoteFanout = multiHashStore.getFileFanout(fileHash);
            if (rootRemoteFanout == null) {
                throw new RuntimeException("Coudlnt find remote hash: " + fileHash);
            }
            List<String> rootHashes = rootRemoteFanout.getHashes();

            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(fTemp);
                try (BufferedOutputStream bufOut = new BufferedOutputStream(fout)) {
                    combiner = new Combiner();
                    // TODO: Use MultipleBlobStore with the httpHashStore and LocalFileTriplet.blobStore to minimise network traffic                
                    combiner.combine(rootHashes, multiHashStore, multiBlobStore, bufOut);
                    bufOut.flush();
                    combiner = null;
                }
            } finally {
                IOUtils.closeQuietly(fout);
            }
        } finally {
            if (partialDownloadBlobStore != null) {
                partialDownloadBlobStore.close();
            }
            if (oldFileBlobStore != null) {
                oldFileBlobStore.close();
            }
        }

        // Verify the CRC
        hashCalc.verifyHash(fTemp, fileHash);

        // Downloaded to temp file, so now swap with real file
        if (localFile.exists()) {
            archiver.archive(localFile);
        }

        // Now rename the new file to the real file name
        if (!fTemp.renameTo(localFile)) {
            throw new RuntimeException("Downloaded update ok, and renamed old file, but failed to rename new file to original file name: " + localFile.getAbsolutePath());
        }

        log.info("Finished update " + localFile.getAbsolutePath());
    }

//    public void deleteRemoteFile(String childEncodedPath) {
//        try {
//            HttpUtils.delete(client, childEncodedPath);
//        } catch (NotFoundException ex) {
//            //System.out.println("Not Found: " + childEncodedPath + " - ignoring exception because we were going to delete it anyway");
//        }
//    }
    public void upSync(Path path) throws FileNotFoundException, IOException {
        if (paused) {
            return;
        }
        File file = toFile(path);

        parser = new Parser();
        InputStream fin = null;
        try {
            File localFile = toFile(path);
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent(localFile));
            fin = new NotifyingFileInputStream(file, progressListener);
            BufferedInputStream bufIn = new BufferedInputStream(fin);

            String newHash;
            Path destPath = baseUrl.add(path);
            if (file.length() < 25000) {
                //log.info("upSync: upload small file: " + file.getAbsolutePath());
                // for a small file its quicker just to upload it     
                boolean done = false;
                int cnt = 0;
                while (!done) {
                    if (cnt++ > 3) {
                        throw new RuntimeException("Uploaded file is not valid: " + destPath);
                    }
                    if (paused) {
                        return;
                    }
                    try {
                        ByteArrayBlobStore byteArrayBlobStore = new ByteArrayBlobStore();
                        newHash = parser.parse(bufIn, new NullHashStore(), byteArrayBlobStore);
                        byte[] data = byteArrayBlobStore.getBytes();
                        HttpResult result = host.doPut(destPath, data, null);
                        if (result.getStatusCode() < 200 || result.getStatusCode() > 299) {
                            throw new IOException("HTTP result code indicates failure: " + result.getStatusCode() + " uploading: " + destPath);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    if (checkRemoteHash(newHash, path)) {
                        done = true;
                    } else {
                        log.warn("Uploaded file, but then couldnt find it: " + destPath);
                    }
                }

            } else {
                log.info("upSync: chunk larger file: " + file.getAbsolutePath());
                newHash = parser.parse(bufIn, httpHashStore, httpBlobStore);

                // Now set the new hash on the remote file, which effectively commits the new content

//                boolean done = false;
//                int cnt = 0;
//                while (!done) {
//                    if (cnt++ > 3) {
//                        throw new RuntimeException("Uploaded file is not valid: " + destPath);
//                    }
                updateHashOnRemoteResource(newHash, path);
//                    done = checkRemoteHash(newHash, path);
//                }
            }
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
            IOUtils.closeQuietly(fin);
            parser = null;
        }

    }

    /**
     * Do a PUT with a special content type so the server knows to just update
     * the file's hash. The PUT content is just the hash
     *
     * @param hash
     * @param encodedPath
     */
    private void updateHashOnRemoteResource(String hash, Path path) {
        // Copy longs into a byte array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            hashCalc.writeHash(hash, bout);
            byte[] data = bout.toByteArray();
            Path p = baseUrl.add(path);
            host.doPut(p, data, "spliffy/hash");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean checkRemoteHash(String expectedFileHash, Path path) {
        Path p = baseUrl.add(path);
        Map<String, String> params = new HashMap<>();
        params.put("type", "hash");
        byte[] arr;
        try {
            arr = host.doGet(p, params);
        } catch (Throwable ex) {
            return false;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        String actualFileHash;
        try {
            actualFileHash = hashCalc.readHash(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        boolean b = expectedFileHash.equals(actualFileHash);
        if (!b) {
            log.error("checkRemoteHash: hashes do not match: " + expectedFileHash + " != " + actualFileHash);
        }
        return b;
    }

    public Path getBaseUrl() {
        return baseUrl;
    }

    private File toFile(Path path) {
        File f = root;
        for (String fname : path.getParts()) {
            f = new File(f, fname);
        }
        return f;
    }

    public void setPaused(boolean state) {
        this.paused = state;
        if (paused) {
            if (combiner != null) {
                combiner.setCanceled(true);
            }
            if (parser != null) {
                parser.setCancelled(true);
            }
        }
    }

    public boolean isReadonlyLocal() {
        return readonlyLocal;
    }

    public void setReadonlyLocal(boolean readonlyLocal) {
        this.readonlyLocal = readonlyLocal;
    }

    private class EventProgressListener implements ProgressListener {

        @Override
        public void onRead(int bytes) {
        }

        @Override
        public void onProgress(long bytesRead, Long totalBytes, String fileName) {
            if (totalBytes != null) {
                EventUtils.fireQuietly(eventManager, new TransferProgressEvent(bytesRead, totalBytes, fileName));
            }
        }

        @Override
        public void onComplete(String fileName) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
