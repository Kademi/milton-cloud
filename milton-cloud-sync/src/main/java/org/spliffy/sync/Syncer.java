package org.spliffy.sync;

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
import io.milton.cloud.common.HashUtils;
import io.milton.event.EventManager;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.MethodNotAllowedException;
import org.spliffy.sync.event.DownloadSyncEvent;
import org.spliffy.sync.event.EventUtils;
import org.spliffy.sync.event.FinishedSyncEvent;
import org.spliffy.sync.event.UploadSyncEvent;

/**
 * This class contains the code to actually perform file sync operations. This
 * is generally called from SyncingDeltaListener in response to file comparison
 * events.
 *
 * @author brad
 */
public class Syncer {
    
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Syncer.class);
    
    private final EventManager eventManager;
    private final HttpHashStore httpHashStore;
    private final HttpBlobStore httpBlobStore;
    private final Host host;
    private final Archiver archiver;
    private final File root;
    private final Path baseUrl;
    
    private boolean paused;
    private boolean readonlyLocal;

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
        Path p = baseUrl.add(path);
        try {
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent());
            host.doMkCol(p);
        } catch (MethodNotAllowedException e) {
            throw new ConflictException(p.toString());
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
        Path p = baseUrl.add(path);
        try {
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent());
            host.doDelete(p);
        } catch (NotFoundException e) {
            // ok
        } catch (IOException | HttpException | NotAuthorizedException | ConflictException | BadRequestException ex) {
            Logger.getLogger(Syncer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
        }
    }

    public void downloadSync(long hash, Path path) throws IOException {
        if( readonlyLocal ) {
            log.warn("Not downsyncing because local is readonly");
            return ;
        }
        try {
            EventUtils.fireQuietly(eventManager, new DownloadSyncEvent());
            _downloadSync(hash, path);;
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
        }
    }
    
    private void _downloadSync(long hash, Path path) throws IOException {
        System.out.println("downloadSync: " + path);
        File localFile = toFile(path);
        List<HashStore> hashStores = new ArrayList<>();
        List<BlobStore> blobStores = new ArrayList<>();

        // If we have a temp file it means a previous download didnt complete. Lets
        // use it as a store so we can resume the download
        File fTemp = new File(localFile.getAbsolutePath() + ".new.tmp");
        FileBlobStore partialDownloadBlobStore = null;
        FileBlobStore oldFileBlobStore = null;
        try {
            if (fTemp.exists()) {
                // found previous download file, so use it as a hashstore
                MemoryHashStore partialDownloadHashStore = new MemoryHashStore();
                partialDownloadBlobStore = new FileBlobStore(fTemp);
                partialDownloadBlobStore.openForRead();
                Parser.parse(fTemp, partialDownloadBlobStore, partialDownloadHashStore);
                hashStores.add(partialDownloadHashStore);
                blobStores.add(partialDownloadBlobStore);
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

            Fanout rootRemoteFanout = multiHashStore.getFanout(hash);
            if (rootRemoteFanout == null) {
                throw new RuntimeException("Coudlnt find remote hash: " + hash);
            }
            List<Long> rootHashes = rootRemoteFanout.getHashes();

            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(fTemp);
                try (BufferedOutputStream bufOut = new BufferedOutputStream(fout)) {
                    Combiner combiner = new Combiner();
                    // TODO: Use MultipleBlobStore with the httpHashStore and LocalFileTriplet.blobStore to minimise network traffic                
                    combiner.combine(rootHashes, multiHashStore, multiBlobStore, bufOut);
                    bufOut.flush();
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
        HashUtils.verifyHash(fTemp, hash);

        // Downloaded to temp file, so now swap with real file
        if (localFile.exists()) {
            archiver.archive(localFile);
        }

        // Now rename the new file to the real file name
        if (!fTemp.renameTo(localFile)) {
            throw new RuntimeException("Downloaded update ok, and renamed old file, but failed to rename new file to original file name: " + localFile.getAbsolutePath());
        }

        System.out.println("Finished update!");
    }

//    public void deleteRemoteFile(String childEncodedPath) {
//        try {
//            HttpUtils.delete(client, childEncodedPath);
//        } catch (NotFoundException ex) {
//            //System.out.println("Not Found: " + childEncodedPath + " - ignoring exception because we were going to delete it anyway");
//        }
//    }
    public void upSync(Path path) throws FileNotFoundException, IOException {
        File file = toFile(path);
        log.info("upSync: " + file.getAbsolutePath());

        FileInputStream fin = null;
        try {
            EventUtils.fireQuietly(eventManager, new UploadSyncEvent());
            fin = new FileInputStream(file);
            BufferedInputStream bufIn = new BufferedInputStream(fin);
            Parser parser = new Parser();
            long newHash = parser.parse(bufIn, httpHashStore, httpBlobStore);

            // Now set the new hash on the remote file, which effectively commits the new content
            updateHashOnRemoteResource(newHash, path);
        } finally {
            EventUtils.fireQuietly(eventManager, new FinishedSyncEvent());
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Do a PUT with a special content type so the server knows to just update
     * the file's hash. The PUT content is just the hash
     *
     * @param hash
     * @param encodedPath
     */
    private void updateHashOnRemoteResource(long hash, Path path) {
        // Copy longs into a byte array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bout);
        try {
            dos.writeLong(hash); // send the actualContentLength first
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        byte[] data = bout.toByteArray();

        try {
            Path p = baseUrl.add(path);            
            host.doPut(p, data, "spliffy/hash");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
        // TODO: should act on uploading and downloading
    }

    public boolean isReadonlyLocal() {
        return readonlyLocal;
    }

    public void setReadonlyLocal(boolean readonlyLocal) {
        this.readonlyLocal = readonlyLocal;
    }
    
    
}
