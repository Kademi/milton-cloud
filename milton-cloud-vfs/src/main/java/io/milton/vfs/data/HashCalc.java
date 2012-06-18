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
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());
        for (ITriplet r : childDirEntries) {
            String name = r.getName();
            String line = HashUtils.toHashableText(r.getName(), r.getHash(), r.getType());
            HashUtils.appendLine(line, cout);
        }
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        return crc;
    }
}
