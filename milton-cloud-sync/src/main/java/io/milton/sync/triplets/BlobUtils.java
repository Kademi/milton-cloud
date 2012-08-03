package io.milton.sync.triplets;

import io.milton.cloud.common.ITriplet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.FileBlobStore;
import io.milton.cloud.common.Triplet;
import io.milton.sync.Utils;

/**
 * Just moving random stuff out of JdbcLocalTripletStore to make it tidier
 *
 * @author brad
 */
public class BlobUtils {

    public static List<ITriplet> toTriplets(File parent, List<CrcDao.CrcRecord> records) {
        List<ITriplet> list = new ArrayList<>();
        for (CrcDao.CrcRecord r : records) {
            File child = new File(parent, r.name);
            if (!child.exists()) {
                // cached information is out of date
                // TODO: should regenerate triplets, but should rarely happen
                throw new RuntimeException("Stale triplet information");
            }
            Triplet t = new Triplet();
            t.setHash(r.crc);
            t.setName(r.name);
            t.setType(Utils.toType(child));
            list.add(t);
        }
        return list;
    }

    public static byte[] loadAndVerify(File currentScanFile, BlobDao.BlobVector v) throws FileNotFoundException, IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(currentScanFile, "r");
            return FileBlobStore.readBytes(raf, v.offset, v.length, v.crc); // implicitly verifies against given crc, will throw IOException if not valid
        } finally {
            IOUtils.closeQuietly(raf);
        }
    }

}
