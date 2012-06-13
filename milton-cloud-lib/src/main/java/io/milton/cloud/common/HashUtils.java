package io.milton.cloud.common;

import java.io.*;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.output.NullOutputStream;
import org.hashsplit4j.api.NullBlobStore;
import org.hashsplit4j.api.NullHashStore;
import org.hashsplit4j.api.Parser;

/**
 *
 * @author brad
 */
public class HashUtils {
    
    public static long calcTreeHash(List<Triplet> triplets) {
        OutputStream nulOut = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());
        Set<String> names = new HashSet<>();
        for (Triplet r : triplets) {
            String name = r.getName();
            if (names.contains(name)) { // defensive check
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
    
    /**
     * 
     * @param name - the name of the resource as it appears withint the current directory
     * @param crc - the hash of the resource. Either the crc of the file, of the hashed value of its members if a directory (ie calculated with this method)
     * @param type - "f" = file, "d" = directory
     * @return 
     */
    public static String toHashableText(String name, Long crc, String type) {
        String line = name + ":" + crc + ":" + type  + '\n';
        return line;
    }

    public static void appendLine(String line, CheckedOutputStream cout) {
        if (line == null) {
            return;
        }
        try {
            cout.write(line.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static List<Triplet> parseTriplets(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in);
        BufferedReader bufIn = new BufferedReader(reader);
        List<Triplet> list = new ArrayList<>();
        String line = bufIn.readLine();
        while( line != null ) {
            Triplet triplet = parse(line);
            list.add(triplet);
            line = bufIn.readLine();
        }
        return list;
    }

    private static Triplet parse(String line) {
        try {
            String[] arr = line.split(":");
            Triplet triplet = new Triplet();
            triplet.setName(arr[0]);
            triplet.setHash(Long.parseLong(arr[1]));
            triplet.setType(arr[2]);
            return triplet;
        } catch (Throwable e) {
            throw new RuntimeException("Couldnt parse - " + line, e);
        }
    }
    
    public static Map<String, Triplet> toMap(List<Triplet> triplets) {
        Map<String,Triplet> map = new HashMap<>();
        for( Triplet t : triplets) {
            map.put(t.getName(), t);
        }
        return map;
    }

    public static void verifyHash(File f, long expectedHash) throws IOException {
        try (FileInputStream fin = new FileInputStream(f); BufferedInputStream bufIn = new BufferedInputStream(fin)) {            
            Parser parser = new Parser();
            NullBlobStore blobStore = new NullBlobStore();
            NullHashStore hashStore = new NullHashStore();
            long actualHash = parser.parse(bufIn, hashStore, blobStore);
            if( actualHash != expectedHash) {
                throw new IOException("File does not have the expected hash value: Expected: " + expectedHash + " actual:" + actualHash );
            }
        }
    }

    public static void appendLine(Date startDate, CheckedOutputStream cout) {
        if( startDate == null ) {
            return ;
        }
        String s= startDate.toString();
        appendLine(s, cout);
    }
}
