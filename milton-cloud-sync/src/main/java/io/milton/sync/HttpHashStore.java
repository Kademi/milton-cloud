package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.HttpResult;
import java.io.*;
import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.FanoutSerializationUtils;
import org.hashsplit4j.api.HashCache;
import org.hashsplit4j.api.HashStore;

/**
 * Implements getting and setting fanout hashes over HTTP
 *
 * Can use an optional HashCache to record knowledge of the existence of objects
 * in the remote repository
 *
 * Reads fanouts as a text file, with the first line being the actual content
 * length of the chunk/file, and then with newlines delimiting hashes
 *
 * @author brad
 */
public class HttpHashStore implements HashStore {

    private final Host host;
    private final HashCache chunksHashCache;
    private final HashCache filesHashCache;
    private int timeout = 30000;
    private Path chunksBasePath;
    private Path filesBasePath;
    private long gets;
    private long sets;
    private boolean force;

    /**
     *
     * @param host
     * @param filesHashCache
     * @param chunksHashCache - optional, may be null. If provided will be used
     * to optimise hasFanout
     */
    public HttpHashStore(Host host, HashCache chunksHashCache, HashCache filesHashCache) {
        this.host = host;
        this.chunksHashCache = chunksHashCache;
        this.filesHashCache = filesHashCache;
    }

    public HttpHashStore(Host host) {
        this.host = host;
        this.chunksHashCache = null;
        this.filesHashCache = null;
    }

    @Override
    public void setChunkFanout(String hash, List<String> childCrcs, long actualContentLength) {
        sets++;

        // Copy longs into a byte array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            FanoutSerializationUtils.writeFanout(childCrcs, actualContentLength, bout);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        byte[] bytes = bout.toByteArray();

        Path destPath = chunksBasePath.child(hash + "");
        HttpResult result = host.doPut(destPath, bytes, null);
        checkResult(result);

        chunksHashCache.setHash(hash);
    }

    @Override
    public Fanout getChunkFanout(String fanoutHash) {
        gets++;
        Path destPath = chunksBasePath.child(fanoutHash + "");
        try {
            byte[] arr = host.doGet(destPath);
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            Fanout fanout = FanoutSerializationUtils.readFanout(bin);

            if (chunksHashCache != null) {
                chunksHashCache.setHash(fanoutHash);
            }
            return fanout;
        } catch (IOException | NotFoundException | HttpException | NotAuthorizedException | BadRequestException | ConflictException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public boolean hasChunk(String fanoutHash) {
        if (chunksHashCache != null) {
            if (chunksHashCache.hasHash(fanoutHash)) { // say that 3 times quickly!!!  :)
                return true;
            } else {
        // If not in the hashcache, lets assume we dont have it.
                // This is faster then doing lots of unnecessary checks
                return false;
            }
        }

        Path destPath = chunksBasePath.child(fanoutHash + "");
        try {
            host.doOptions(destPath);
            if (chunksHashCache != null) {
                chunksHashCache.setHash(fanoutHash);
            }
            return true;
        } catch (NotFoundException ex) {
            return false;
        } catch (IOException | HttpException | NotAuthorizedException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void setFileFanout(String hash, List<String> fanoutHashes, long actualContentLength) {
        if (!force && hasFile(hash)) {
            return;
        }

        sets++;

        // Copy longs into a byte array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            FanoutSerializationUtils.writeFanout(fanoutHashes, actualContentLength, bout);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        byte[] bytes = bout.toByteArray();

        Path destPath = filesBasePath.child(hash + "");
        HttpResult result = host.doPut(destPath, bytes, null);
        checkResult(result);

        if (filesHashCache != null) {
            filesHashCache.setHash(hash);
        }
    }

    @Override
    public Fanout getFileFanout(String fileHash) {
        gets++;
        Path destPath = filesBasePath.child(fileHash + "");
        try {
            byte[] arr = host.doGet(destPath);
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);

            String s = new String(arr);
            Fanout fanout = FanoutSerializationUtils.readFanout(bin);

            if (filesHashCache != null) {
                if (fanout != null) {
                    filesHashCache.setHash(fileHash);
                }
            }
            return fanout;
        } catch (IOException | NotFoundException | HttpException | NotAuthorizedException | BadRequestException | ConflictException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasFile(String fileHash) {
        if (filesHashCache != null) {
            if (filesHashCache.hasHash(fileHash)) { // say that 3 times quickly!!!  :)
                return true;
            }
            // assume we dont have it, not necessarily true but faster then unnecessary checks
            return false;
        }
        gets++;
        Path destPath = filesBasePath.child(fileHash + "");
        try {
            host.doOptions(destPath);
            if (filesHashCache != null) {
                filesHashCache.setHash(fileHash);
            }
            return true;
        } catch (NotFoundException ex) {
            return false;
        } catch (IOException | HttpException | NotAuthorizedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Base url to PUT to, hash will be appended. Must end with a slash
     *
     * Eg http://myserver/blobs
     *
     * @return
     */
    public String getChunksBaseUrl() {
        return chunksBasePath.toString();
    }

    public void setChunksBaseUrl(String baseUrl) {
        this.chunksBasePath = Path.path(baseUrl);
    }

    public String getFilesBasePath() {
        return filesBasePath.toString();
    }

    public void setFilesBasePath(String filesBasePath) {
        this.filesBasePath = Path.path(filesBasePath);
    }

    public long getGets() {
        return gets;
    }

    public long getSets() {
        return sets;
    }

    private void checkResult(HttpResult result) {
        if (result.getStatusCode() < 200 || result.getStatusCode() > 299) {
            throw new RuntimeException("Failed to upload - " + result.getStatusCode());
        }
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

}
