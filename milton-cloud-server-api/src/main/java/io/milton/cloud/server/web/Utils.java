package io.milton.cloud.server.web;

import io.milton.resource.Resource;
import java.util.*;

/**
 *
 * @author brad
 */
public class Utils {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Utils.class);

    public static Resource childOf(List<? extends Resource> children, String name) {
        if (children == null) {
            return null;
        }
        for (Resource r : children) {
            if (r.getName().equals(name)) {
                return r;
            }
        }
        return null;
    }

}
