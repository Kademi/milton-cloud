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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements getting and setting blobs over HTTP
 *
 * @author brad
 */
public class HttpBlobStore implements BlobStore {

    private static final Logger log = LoggerFactory.getLogger(HttpBlobStore.class);

    private final Host host;
    private final HashCache hashCache;
    private int timeout = 30000;
    private Path basePath;
    private long gets;
    private long sets;
    private boolean force;

    public HttpBlobStore(String server, int port, String rootPath, String username, String password) {
        this.host = new Host(server, port, username, password, null);
        this.host.setUseDigestForPreemptiveAuth(false); // we don't want DIGEST because it precludes preemptive auth
        this.hashCache = new MemoryHashCache();
        this.basePath = Path.path(rootPath);
    }

    public HttpBlobStore(Host host, HashCache hashCache) {
        this.host = host;
        this.hashCache = hashCache;
    }

    public HttpBlobStore(Host host) {
        this.host = host;
        this.hashCache = null;
    }

    @Override
    public void setBlob(String hash, byte[] bytes) {
        if ( !force && hashCache.hasHash(hash)) {
            log.info("setBlob: Hash cache already has hash {}, and force is false, so will not upload", hash);
            return;
        }

        Path destPath = basePath.child(hash + "");
        log.info("PUT " + hash);
        HttpResult result = host.doPut(destPath, bytes, null);
        checkResult(result);
        sets++;
        if (hashCache != null) {
            hashCache.setHash(hash);
        }

    }

    @Override
    public boolean hasBlob(String hash) {
        if (hashCache != null) {
            if (hashCache.hasHash(hash)) { // say that 3 times quickly!!!  :)
                return true;
            } else {
                return false;
            }
        }
        Path destPath = basePath.child(hash + "");
        try {
            gets++;
            host.doOptions(destPath);
            if (hashCache != null) {
                hashCache.setHash(hash);
            }
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getBlob(String hash) {
        gets++;
        Path destPath = basePath.child(hash + "");
        try {
            byte[] arr = host.doGet(destPath);
            if( arr != null ) {
                if( hashCache != null ) {
                    hashCache.setHash(hash);
                }
            }
            return arr;
        } catch (NotFoundException e) {
            return null;
        } catch (IOException | HttpException | NotAuthorizedException | BadRequestException | ConflictException ex) {
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
     * Eg http://myserver/blobs/
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
