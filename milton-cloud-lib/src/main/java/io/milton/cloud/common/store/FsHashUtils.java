package io.milton.cloud.common.store;

import java.io.File;

/**
 *
 * @author brad
 */
public class FsHashUtils {
    public static File toFile(File root, String hex) {        
        File f = root;
        String name = hex;
        while( name.length() > 6) {
            String subdir = name.substring(0, 6);
            f = new File(f, subdir);
            name = name.substring(6);
        }
        return new File(f, hex);
    }
}
