package io.milton.sync;

import io.milton.common.Path;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.triplets.ITriplet;

/**
 *
 * @author brad
 */
public class Utils {

    public static boolean ignored(File childFile) {
        return ignored(childFile, null);
    }

    public static boolean ignored(File childFile, List<String> ignoredPatterns) {
        if( childFile.getName().equals(".ksync") ) { // usually ignore resources starting with a dot, but special case for .mil directory
            return true;
        }
        if( ignoredPatterns != null ) {
            for( String s : ignoredPatterns) {
                if( childFile.getName().equals(s) || childFile.getName().matches(s)) {
                    return true;
                }
            }
        }
        return childFile.isHidden() || childFile.getName().startsWith(".");
    }

    public static boolean ignored(String name) {
        if( name == null ) {
            return false; //indicates the root of Path
        }
        if( name.equals(".mil") ) { // usually ignore resources starting with a dot, but special case for .mil directory
            return false;
        }
        return name.startsWith(".");
    }

    public static boolean ignored(Path p) {
        while (p != null && p.getName() != null ) {
            if (Utils.ignored(p.getName())) {
                return true;
            }
            p = p.getParent();
        }
        return false;
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

    public static Map<String, ITriplet> toMap(List<ITriplet> triplets) {
        Map<String, ITriplet> map = new HashMap<>();
        if (triplets != null) {
            for (ITriplet r : triplets) {
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
