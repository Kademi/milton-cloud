package io.milton.cloud.server.web;

import io.milton.vfs.db.ItemHistory;
import io.milton.vfs.db.DataItem;
import io.milton.vfs.db.MetaItem;
import io.milton.vfs.db.SessionManager;
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

    /**
     * Produce a web resource representation of the given ItemHistory.
     *
     * This will be either a FileResource or a DirectoryResource, depending on
     * the type associated with the member
     *
     * @param parent
     * @param dm
     * @return
     */
    public static MutableResource toResource(MutableCollection parent, ItemHistory dm, boolean renderMode) {
        MetaItem itemVersion = dm.getMemberItem();
        String type = itemVersion.getItem().getType();

        switch (type) {
            case "d":
                DirectoryResource rdr = new DirectoryResource(dm.getName(), itemVersion, parent, parent.getServices(), renderMode);
                rdr.setHash(dm.getMemberItem().getItemHash());
                rdr.setDirectoryMember(dm);
                return rdr;
            case "f":
                FileResource rfr = new FileResource(dm.getName(), itemVersion, parent, parent.getServices());
                rfr.setHash(dm.getMemberItem().getItemHash());
                rfr.setDirectoryMember(dm);
                if (renderMode) {
                    if (isHtml(rfr)) {
                        return new RenderFileResource(parent.getServices(), rfr);
                    }
                    return rfr;
                } else {
                    return rfr;
                }
            default:
                throw new RuntimeException("Unknown resource type: " + type);
        }
    }


    private static boolean isHtml(FileResource rfr) {
        String ct = rfr.getContentType("text/html"); // find if it can produce html
        return "text/html".equals(ct);
    }
}
