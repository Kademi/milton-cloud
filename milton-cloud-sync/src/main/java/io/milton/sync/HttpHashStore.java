package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.FanoutImpl;
import org.hashsplit4j.api.HashCache;
import org.hashsplit4j.api.HashStore;

/**
 * Implements getting and setting fanout hashes over HTTP
 *
 * Can use an optional HashCache to record knowledge of the existence of objects
 * in the remote repository
 *
 * @author brad
 */
public class HttpHashStore implements HashStore {

    private final Host host;
    private final HashCache hashCache;
    private int timeout = 30000;
    private Path basePath;
    private long gets;
    private long sets;

    /**
     *
     * @param client
     * @param hashCache - optional, may be null. If provided will be used to
     * optimise hasFanout
     */
    public HttpHashStore(Host host, HashCache hashCache) {
        this.host = host;
        this.hashCache = hashCache;
    }

    @Override
    public void setFanout(long hash, List<Long> childCrcs, long actualContentLength) {
        if (hasFanout(hash)) {
            return;
        }

        sets++;

        // Copy longs into a byte array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bout);
        try {
            dos.writeLong(actualContentLength); // send the actualContentLength first
            for (Long l : childCrcs) {
                dos.writeLong(l);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        byte[] bytes = bout.toByteArray();

        Path destPath = basePath.child(hash + "");
        host.doPut(destPath, bytes, null);
    }

    @Override
    public Fanout getFanout(long fanoutHash) {
        gets++;
        Path destPath = basePath.child(fanoutHash + "");
        try {
            byte[] arr = host.doGet(destPath);
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            List<Long> list = new ArrayList<>();
            DataInputStream din = new DataInputStream(bin);
            long actualContentLength = din.readLong();
            try {
                while (true) {
                    list.add(din.readLong());
                }
            } catch (EOFException e) {
                // cool
            }

            if (hashCache != null) {
                hashCache.setHash(fanoutHash);
            }
            return new FanoutImpl(list, actualContentLength);
        } catch (IOException | NotFoundException | HttpException | NotAuthorizedException | BadRequestException | ConflictException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public boolean hasFanout(long fanoutHash) {
        if (hashCache != null) {
            if (hashCache.hasHash(fanoutHash)) { // say that 3 times quickly!!!  :)
                return true;
            }
        }
        Path destPath = basePath.child(fanoutHash + "");
        try {
            host.doOptions(destPath);
            if (hashCache != null) {
                hashCache.setHash(fanoutHash);
            }
            return true;
        } catch (NotFoundException ex) {
            return false;
        } catch ( IOException | HttpException | NotAuthorizedException ex) {
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
}
