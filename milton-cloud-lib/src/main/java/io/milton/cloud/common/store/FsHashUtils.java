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
        while( name.length() > 3) {
            String subdir = name.substring(0, 3);
            f = new File(f, subdir);
            name = name.substring(3);
        }
        return new File(f, hex);
    }
}
