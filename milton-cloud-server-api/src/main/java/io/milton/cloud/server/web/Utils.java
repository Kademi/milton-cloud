package io.milton.cloud.server.web;

import io.milton.resource.Resource;
import java.util.*;

/**
 *
 * @author brad
 */
public class Utils {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Utils.class);

    public static String stripSuffix(String host, String suffix) {
        return host.substring(0, host.length()-suffix.length());
    }

    public static String stripPrefix(String subdomain, String prefix) {
        return subdomain.substring(prefix.length());
    }

}
