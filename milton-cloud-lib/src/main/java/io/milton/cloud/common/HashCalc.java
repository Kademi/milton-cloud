package io.milton.cloud.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.output.NullOutputStream;
import org.hashsplit4j.api.NullBlobStore;
import org.hashsplit4j.api.NullHashStore;
import org.hashsplit4j.api.Parser;

/**
 *
 * @author brad
 */
public class HashCalc {

    private static final HashCalc hashCalc = new HashCalc();
    private static final ITripletComparator COMPARATOR = new ITripletComparator();

    public static HashCalc getInstance() {
        return hashCalc;
    }

    public String calcHash(Iterable<? extends ITriplet> childDirEntries) throws IOException {
        OutputStream nulOut = new NullOutputStream();
        return calcHash(childDirEntries, nulOut);
    }

    public String calcHash(Iterable<? extends ITriplet> childDirEntries, OutputStream out) throws IOException {
        MessageDigest crypto = Parser.getCrypt();
        for (ITriplet r : childDirEntries) {
            String line = toHashableText(r.getName(), r.getHash(), r.getType());
            appendLine(line, crypto);
            out.write(line.getBytes());
        }
        return Parser.toHex(crypto);
    }

    /**
     *
     * @param name - the name of the resource as it appears withint the current
     * directory
     * @param crc - the hash of the resource. Either the crc of the file, of the
     * hashed value of its members if a directory (ie calculated with this
     * method)
     * @param type - "f" = file, "d" = directory
     * @return
     */
    public static String toHashableText(String name, String crc, String type) {
        String line = name + ":" + crc + ":" + type + '\n';
        return line;
    }

    public static void appendLine(String line, MessageDigest cout) {
        if (line == null) {
            return;
        }
        cout.update(line.getBytes());

    }

    public List<ITriplet> parseTriplets(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in);
        BufferedReader bufIn = new BufferedReader(reader);
        List<ITriplet> list = new ArrayList<>();
        String line = bufIn.readLine();
        while (line != null) {
            Triplet triplet = parse(line);
            list.add(triplet);
            line = bufIn.readLine();
        }
        return list;
    }

    private Triplet parse(String line) {
        try {
            String[] arr = line.split(":");
            Triplet triplet = new Triplet();
            triplet.setName(arr[0]);
            triplet.setHash(arr[1]);
            triplet.setType(arr[2]);
            return triplet;
        } catch (Throwable e) {
            throw new RuntimeException("Couldnt parse - " + line, e);
        }
    }

    public Map<String, ITriplet> toMap(List<ITriplet> triplets) {
        Map<String, ITriplet> map = new HashMap<>();
        for (ITriplet t : triplets) {
            map.put(t.getName(), t);
        }
        return map;
    }

    public void verifyHash(File f, String expectedHash) throws IOException {
        try (FileInputStream fin = new FileInputStream(f)) {
            verifyHash(fin, expectedHash);
        }
    }
    
    public void verifyHash(InputStream fin, String expectedHash) throws IOException {
        try (BufferedInputStream bufIn = new BufferedInputStream(fin)) {
            Parser parser = new Parser();
            NullBlobStore blobStore = new NullBlobStore();
            NullHashStore hashStore = new NullHashStore();
            String actualHash = parser.parse(bufIn, hashStore, blobStore);
            if (!actualHash.equals(expectedHash)) {
                throw new IOException("File does not have the expected hash value: Expected: " + expectedHash + " actual:" + actualHash);
            }
        }
    }    

    public void sort(List<? extends ITriplet> list) {
        Collections.sort(list, COMPARATOR);
    }

    /**
     * Just reads a hex formatted SHA1 hash from the inputstream. Assumes that the
     * string is the first line
     * 
     * @param in
     * @return
     * @throws IOException 
     */
    public String readHash(InputStream in) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(r);
        String hash = br.readLine();
        return hash;
    }    
    
    public void writeHash(String hash, OutputStream out) throws IOException {
        OutputStreamWriter w = new OutputStreamWriter(out);
        BufferedWriter bw = new BufferedWriter(w);
        bw.write(hash);
        bw.newLine();
        bw.flush();
        w.flush();
        out.flush();
    }
    
    public static class ITripletComparator implements Comparator<ITriplet> {

        @Override
        public int compare(ITriplet o1, ITriplet o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (o1.getName().equals(o2.getName())) {
                    // name is equal, so differentiate on type
                    return o1.getType().compareTo(o2.getType());
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        }
    }
}
