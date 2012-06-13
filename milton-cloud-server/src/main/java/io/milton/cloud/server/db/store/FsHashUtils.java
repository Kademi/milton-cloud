package io.milton.cloud.server.db.store;

import java.io.File;

/**
 *
 * @author brad
 */
public class FsHashUtils {
    public static String toHex(long hash) {
        String hex = Long.toHexString(hash);
        return hex;
    }

    public static File toFile(File root, long hash) {
        File f = root;
        String hex = toHex(hash);
        String name = hex;
        while( name.length() > 3) {
            String subdir = name.substring(0, 2);
            f = new File(f, subdir);
            name = name.substring(2);
        }
        return new File(f, hex);
    }
}
