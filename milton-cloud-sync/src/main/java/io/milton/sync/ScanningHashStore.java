package io.milton.sync;

import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.*;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.output.NullOutputStream;
import org.hashsplit4j.api.*;
import io.milton.cloud.common.Triplet;
import io.milton.cloud.common.HashUtils;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;

/**
 *
 * @author brad
 */
public class ScanningHashStore implements HashStore {

    private final Host httpClient;
    private final Path basePath;
    private final File local;
    private Triplet rootTriplet = new Triplet();
    private Map<File, LocalFileTriplet> mapOfLocalTriplets = new HashMap<>();
    private final MemoryHashStore hashStore = new MemoryHashStore();

    public ScanningHashStore(Host httpClient, File local, String baseUrl) {
        this.httpClient = httpClient;
        this.local = local;
        this.basePath = Path.path(baseUrl);
    }

    public long scan() throws IOException, HttpException, NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
        return walkTree(local, rootTriplet, basePath);
    }

    public LocalFileTriplet getLocalTriplet(File localFile) {
        return mapOfLocalTriplets.get(localFile);
    }

    /**
     *
     * @param dir - the local directory being scanned
     * @param dirTriplet - triplet representing the directory being scanner
     * @param dirPath - the percentage encoded path of the remote
     * repository
     * @return - the hash of the directory being scanned
     * @throws IOException
     */
    private long walkTree(File dir, Triplet dirTriplet, Path dirPath) throws IOException, HttpException, NotAuthorizedException, BadRequestException, ConflictException {
        Map<String, Triplet> mapOfRemoteTriplets;
        try {
            Map<String,String> params = new HashMap<>();
            params.put("type", "hashes");
            Path p = basePath.add(dirPath);
            byte[] arrRemoteTriplets = httpClient.doGet(p, params);            
            List<Triplet> triplets = HashUtils.parseTriplets(new ByteArrayInputStream(arrRemoteTriplets));
            mapOfRemoteTriplets = HashUtils.toMap(triplets);
        } catch (NotFoundException ex) {
            mapOfRemoteTriplets = new HashMap<>();
        }


        // Now scan local files
        List<File> files = orderedList(dir.listFiles());
        List<Triplet> fileTriplets = new ArrayList<>();
        for (File childFile : files) {
            if (!ignored(childFile)) {
                String name = childFile.getName();
                LocalFileTriplet childTriplet = new LocalFileTriplet(childFile);
                mapOfLocalTriplets.put(childFile, childTriplet);
                fileTriplets.add(childTriplet);
                childTriplet.setName(name);
                childTriplet.setType(childFile.isDirectory() ? "d" : "f");
                long hash;
                if (childFile.isDirectory()) {
                    Path childPath = dirPath.child(childFile.getName());
                    hash = walkTree(childFile, childTriplet, childPath);
                } else {
                    hash = parseFile(childFile, childTriplet.getBlobStore());
                }
                childTriplet.setHash(hash);
                Triplet remoteTriplet = mapOfRemoteTriplets.get(name);
            }
        }
        long hash = calcTreeHash(fileTriplets);
        dirTriplet.setChildren(fileTriplets);
        return hash;
    }

    private List<File> orderedList(File[] listFiles) {
        List<File> list = new ArrayList<>();
        if (listFiles != null) {
            list.addAll(Arrays.asList(listFiles));
            Collections.sort(list);
        }
        return list;
    }

    private long parseFile(File childFile, BlobStore blobStore) throws IOException {
        Parser parser = new Parser();
        FileInputStream fin = new FileInputStream(childFile);
        long hash = parser.parse(fin, hashStore, blobStore);
        return hash;
    }

    private long calcTreeHash(List<Triplet> fileTriplets) {
        OutputStream nulOut = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());
        Set<String> names = new HashSet<>();
        for (Triplet r : fileTriplets) {
            String name = r.getName();
            if (names.contains(name)) {
                throw new RuntimeException("Name not unique within collection: " + name);
            }
            names.add(name);
            String line = HashUtils.toHashableText(name, r.getHash(), r.getType());
            HashUtils.appendLine(line, cout);
        }
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        return crc;
    }

    @Override
    public void setFanout(long hash, List<Long> childCrcs, long actualContentLength) {
        hashStore.setFanout(hash, childCrcs, actualContentLength);
    }

    @Override
    public Fanout getFanout(long fanoutHash) {
        return hashStore.getFanout(fanoutHash);
    }

    @Override
    public boolean hasFanout(long fanoutHash) {
        return hashStore.hasFanout(fanoutHash);
    }

    public boolean ignored(File childFile) {
        return Utils.ignored(childFile);
    }
}
