package io.milton.cloud.server.web;

import io.milton.resource.Resource;
import io.milton.vfs.content.ContentSession.ContentNode;
import io.milton.vfs.content.ContentSession.DirectoryNode;
import io.milton.vfs.content.ContentSession.FileNode;
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
    public static CommonResource toResource(ContentDirectoryResource parent, ContentNode contentNode, boolean renderMode) {
        if (contentNode instanceof DirectoryNode) {
            DirectoryNode dm = (DirectoryNode) contentNode;
            DirectoryResource rdr = new DirectoryResource(dm, parent, parent.getServices(), renderMode);
            return rdr;
        } else if (contentNode instanceof FileNode) {
            FileNode dm = (FileNode) contentNode;
            FileResource rfr = new FileResource(dm, parent, parent.getServices());
            if (renderMode) {
                if (isHtml(rfr)) {
                    return new RenderFileResource(parent.getServices(), rfr);
                }
                return rfr;
            } else {
                return rfr;
            }
        } else {
            throw new RuntimeException("Unknown resource type: " + contentNode);
        }
    }

    private static boolean isHtml(FileResource rfr) {
        String ct = rfr.getContentType("text/html"); // find if it can produce html
        return "text/html".equals(ct);
    }

    public static ResourceList toResources(ContentDirectoryResource parent, DirectoryNode dir, boolean renderMode) {
        ResourceList list = new ResourceList();                
        for( ContentNode n : dir.getChildren()) {
            CommonResource r = toResource(parent, n, renderMode);
            list.add(r);
        }
        return list;
    }
}
