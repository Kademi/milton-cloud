package io.milton.sync;

import io.milton.common.Path;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.milton.cloud.common.Triplet;

/**
 *
 * @author brad
 */
public class Utils {
    public static boolean ignored(File childFile) {
        if( childFile.getName().equals(".mil")) { // usually ignore resources starting with a dot, but special case for .mil directory
            return false;
        }
        return childFile.isHidden() || childFile.getName().startsWith(".");
    }
    
    public static Map<String, File> toMap(File[] files) {
        Map<String, File> map = new HashMap<>();
        if (files != null) {
            for (File r : files) {
                map.put(r.getName(), r);
            }
        }
        return map;
    }
    
    public static Map<String, Triplet> toMap(List<Triplet> triplets) {
        Map<String, Triplet> map = new HashMap<>();
        if (triplets != null) {
            for (Triplet r : triplets) {
                map.put(r.getName(), r);
            }
        }
        return map;
    }    

    public static File toFile(File root, Path path) {
        File f = root;
        for (String fname : path.getParts()) {
            f = new File(f, fname);
        }
        return f;
    }

    public static String toType(File child) {
        return child.isDirectory() ? "d" : "f";
    }

}
