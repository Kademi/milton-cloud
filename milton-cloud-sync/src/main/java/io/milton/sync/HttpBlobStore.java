package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.HttpResult;
import java.io.IOException;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashCache;

/**
 * Implements getting and setting blobs over HTTP
 *
 * @author brad
 */
public class HttpBlobStore implements BlobStore {

    private final Host host;
    private final HashCache hashCache;
    private int timeout = 30000;
    private Path basePath;
    private long gets;
    private long sets;

    public HttpBlobStore(String server, int port, String rootPath, String username, String password) {
        this.host = new Host(server, rootPath, port, username, password, null, null);
        this.hashCache = new MemoryHashCache();
    }

    
    
    public HttpBlobStore(Host host, HashCache hashCache) {
        this.host = host;
        this.hashCache = hashCache;
    }

    @Override
    public void setBlob(String hash, byte[] bytes) {
        if (hasBlob(hash)) {
            return;
        }
        Path destPath = basePath.child(hash + "");
        HttpResult result = host.doPut(destPath, bytes, null);
        checkResult(result);
    }

    @Override
    public boolean hasBlob(String hash) {
        if (hashCache != null) {
            if (hashCache.hasHash(hash)) { // say that 3 times quickly!!!  :)
                return true;
            }
        }
        Path destPath = basePath.child(hash + "");
        try {
            host.doOptions(destPath);
            if (hashCache != null) {
                hashCache.setHash(hash);
            }
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getBlob(String hash) {
        Path destPath = basePath.child(hash + "");
        try {
            return host.doGet(destPath);
        } catch (IOException | NotFoundException | HttpException | NotAuthorizedException | BadRequestException | ConflictException ex) {
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
    public String getBaseUrl() {
        return basePath.toString();
    }

    public void setBaseUrl(String baseUrl) {
        this.basePath = Path.path(baseUrl);
    }

    public long getGets() {
        return gets;
    }

    public long getSets() {
        return sets;
    }
    
    private void checkResult(HttpResult result) {
        if (result.getStatusCode() < 200 || result.getStatusCode() > 299 ) {
            throw new RuntimeException("Failed to upload - " + result.getStatusCode());
        }

    }    
}
