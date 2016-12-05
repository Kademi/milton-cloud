/* 
 *       Copyright FuseLMS
 */
package io.milton.sync.triplets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.BlobImpl;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dylan
 */
public class FastParser extends Parser {

    private static final Logger log = LoggerFactory.getLogger(FastParser.class);
    private final BlockingQueue<BlobImpl> queue;

    public FastParser() {
        this.queue = new ArrayBlockingQueue<>(10);
    }

    public static String parse(File f, BlobStore blobStore, HashStore hashStore) throws FileNotFoundException, IOException {
        FastParser fParser = new FastParser();
        FileInputStream fin = null;
        BufferedInputStream bufIn = null;
        try {
            fin = new FileInputStream(f);
            bufIn = new BufferedInputStream(fin);
            return fParser.parse(bufIn, hashStore, blobStore);
        } finally {
            IOUtils.closeQuietly(bufIn);
            IOUtils.closeQuietly(fin);
        }
    }

    @Override
    public String parse(InputStream in, HashStore hashStore, BlobStore blobStore) throws IOException {
        Date start = new Date();
        MultithreadBlobStore multithreadBlobStore = new MultithreadBlobStore(blobStore);
        String hash = super.parse(in, hashStore, multithreadBlobStore);

        while (!multithreadBlobStore.isComplete()) {
        }
        multithreadBlobStore.stop();

        Date end = new Date();

        long totalTime = end.getTime() - start.getTime();

        log.info("Processed file in {} milliseconds", totalTime);

        return hash;
    }

    public class MultithreadBlobStore implements BlobStore {

        private final BlobStore blobStore;
        private final ExecutorService exService;
        private final List<BlobStoreRunnable> blobStoreRunnables;

        public MultithreadBlobStore(final BlobStore blobStore) {
            this.blobStore = blobStore;
            exService = Executors.newFixedThreadPool(10);
            blobStoreRunnables = new ArrayList<>();
            populateThreadpool();
        }

        @Override
        public void setBlob(String hash, byte[] bytes) {
            try {
                log.info("Adding blob to queue: {}", hash);
                BlobImpl blob = new BlobImpl(hash, bytes);
                queue.put(blob);
            } catch (InterruptedException ex) {
            }
        }

        @Override
        public byte[] getBlob(String string) {
            return blobStore.getBlob(string);
        }

        @Override
        public boolean hasBlob(String string) {
            return blobStore.hasBlob(string);
        }

        public boolean isComplete() {

            return queue.isEmpty();
        }

        private void populateThreadpool() {
            for (int i = 0; i < 10; i++) {
                BlobStoreRunnable blobStoreRunnable = new BlobStoreRunnable(blobStore);
                exService.execute(blobStoreRunnable);
            }
        }

        public void stop() {
            for (BlobStoreRunnable blobStoreRunnable : blobStoreRunnables) {
                blobStoreRunnable.stopLoop();
            }
        }
    }

    public class BlobStoreRunnable extends Thread {

        private final BlobStore blobStore;
        private boolean running = true;

        public BlobStoreRunnable(final BlobStore blobStore) {
            this.blobStore = blobStore;
        }

        @Override
        public void run() {
            BlobImpl blob;
            while (running) {
                try {
                    blob = queue.take();
                    if (blob != null) {
                        blobStore.setBlob(blob.getHash(), blob.getBytes());
                        log.info("Storing blob into store: {}", blob.getHash());
                    }
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        log.error("An InterruptedException was thrown with queue {}", queue, ex);
                        throw new RuntimeException(ex);
                    } else {
                        log.error("Exception inserting blob into store:{}", blobStore, ex);
                    }
                }
            }
        }

        public void stopLoop() {
            running = false;
        }
    }
}
