package org.spliffy.server.db.store;

import io.milton.cloud.common.store.FsHashUtils;
import java.io.File;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class FsHashUtilsTest {


    /**
     * Test of toFile method, of class FsHashUtils.
     */
    @Test
    public void testToFile() {
        long hash = 1335005158161l;  // 136d4821b11
        File root = new File("/tmp");
        File dest = FsHashUtils.toFile(root, hash);
        System.out.println("dest: " + dest.getAbsolutePath());
    }
}
