package io.milton.cloud.server.web;

import io.milton.cloud.common.HashUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.apache.commons.io.output.NullOutputStream;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.Item;
import io.milton.cloud.server.db.ItemVersion;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class HashCalc {

    public static long calcHash(List<DirectoryMember> childDirEntries) {
        OutputStream nulOut = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());
        Set<String> names = new HashSet<>();
        for (DirectoryMember r : childDirEntries) {
            String name = r.getName();
            if (names.contains(name)) {
                throw new RuntimeException("Name not unique within collection: " + name);
            }
            names.add(name);
            ItemVersion version = r.getMemberItem();
            Item item = version.getItem();
            String line = HashUtils.toHashableText(name, version.getItemHash(), item.getType());
            HashUtils.appendLine(line, cout);
        }
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        return crc;
    }

    public static long calcResourceesHash(List<? extends Resource> children) {
        OutputStream nulOut = new NullOutputStream();
        try {
            return calcResourceesHash(children, nulOut);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Calculate the directory hash, outputting hashed text to the given stream
     *
     * @param children
     * @param out
     * @return
     */
    public static long calcResourceesHash(List<? extends Resource> children, OutputStream out) throws IOException {
        CheckedOutputStream cout = new CheckedOutputStream(out, new Adler32());
        for (Resource r : children) {
            if (r instanceof MutableResource) {
                MutableResource mr = (MutableResource) r;
                String type = (r instanceof MutableCollection) ? "d" : "f";
                String line = HashUtils.toHashableText(r.getName(), mr.getEntryHash(), type);
                HashUtils.appendLine(line, cout);
            }
        }
        cout.flush();
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        return crc;
    }
}
