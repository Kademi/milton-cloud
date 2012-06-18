package io.milton.vfs.data;

import io.milton.cloud.common.HashUtils;
import io.milton.cloud.common.ITriplet;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.output.NullOutputStream;

/**
 *
 * @author brad
 */
public class HashCalc {

    private static final HashCalc hashCalc = new HashCalc();
    
    public static HashCalc getInstance() {
        return hashCalc;
    }
    
    public long calcHash(Iterable<? extends ITriplet> childDirEntries) {
        OutputStream nulOut = new NullOutputStream();
        return calcHash(childDirEntries, nulOut);
    }
    public long calcHash(Iterable<? extends ITriplet> childDirEntries, OutputStream out) {        
        CheckedOutputStream cout = new CheckedOutputStream(out, new Adler32());
        for (ITriplet r : childDirEntries) {
            String line = HashUtils.toHashableText(r.getName(), r.getHash(), r.getType());
            HashUtils.appendLine(line, cout);
        }
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        return crc;
    }
}
