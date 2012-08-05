package io.milton.cloud.common.store;

import java.io.*;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.BlobStore;

/**
 * Stores blobs straight into a file system
 *
 * @author brad
 */
public class FileSystemBlobStore implements BlobStore{

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileSystemBlobStore.class);
    
    private final File root;

    public FileSystemBlobStore(File root) {
        this.root = root;
    }
        
    
    @Override
    public void setBlob( String hash, byte[] bytes) {
        File blob = FsHashUtils.toFile(root, hash);
        if (blob.exists()) {
            log.trace("FileSystemBlobStore: setBlob: file exists: {}", blob.getAbsolutePath());
            return; // already exists, so dont overwrite
        }
        File dir = blob.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Couldnt create blob directory: " + dir.getAbsolutePath());
            }
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(blob);
            fout.write(bytes);
            fout.flush();
        } catch (IOException ex) {
            throw new RuntimeException(blob.getAbsolutePath(), ex);
        } finally {
            IOUtils.closeQuietly(fout);
        }
        log.trace("FileSystemBlobStore: setBlob: wrote file: {} with bytes: {}", blob.getAbsolutePath(),  bytes.length);
    }

    @Override
    public byte[] getBlob( String hash) {
        File blob = FsHashUtils.toFile(root, hash);
        if (!blob.exists()) {
            return null;
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(blob);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy(fin, bout);
            byte[] arr = bout.toByteArray();
            log.trace("FileSystemBlobStore: getBlob: loaded file: {} for hash: {}",blob.getAbsolutePath(), hash);
            return arr;
        } catch (IOException ex) {
            throw new RuntimeException(blob.getAbsolutePath(), ex);
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    @Override
    public boolean hasBlob(String hash) {
        File blob = FsHashUtils.toFile(root, hash);
        return blob.exists();
    }
}
