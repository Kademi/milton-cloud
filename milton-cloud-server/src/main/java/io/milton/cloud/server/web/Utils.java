package io.milton.cloud.server.web;

import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.Item;
import io.milton.cloud.server.db.ItemVersion;
import io.milton.cloud.server.db.utils.SessionManager;
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
     * Produce a web resource representation of the given DirectoryMember.
     *
     * This will be either a FileResource or a DirectoryResource, depending on
     * the type associated with the member
     *
     * @param parent
     * @param dm
     * @return
     */
    public static MutableResource toResource(MutableCollection parent, DirectoryMember dm, boolean renderMode) {
        ItemVersion itemVersion = dm.getMemberItem();
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

    public static ResourceList toResources(MutableCollection parent, List<DirectoryMember> dirEntries, boolean renderMode) {
        ResourceList list = new ResourceList();
        Set<String> names = new HashSet<>();
        if (dirEntries != null) {
            for (DirectoryMember dm : dirEntries) {
                String name = dm.getName();
                if (names.contains(name)) {
                    throw new RuntimeException("Name not unique within collection: " + name);
                }
                names.add(name);

                MutableResource r = Utils.toResource(parent, dm, renderMode);
                list.add(r);
            }
        }
        return list;
    }

    public static ItemVersion newDirItemVersion() {
        return newItemVersion((Item) null, "d");
    }

    public static ItemVersion newDirItemVersion(Item item) {
        return newItemVersion(item, "d");
    }

    public static ItemVersion newFileItemVersion() {
        return newItemVersion((Item) null, "f");
    }

    public static ItemVersion newFileItemVersion(Item item) {
        return newItemVersion(item, "f");
    }

    public static ItemVersion newItemVersion(ItemVersion currentVersion, String type) {
        Item item = null;
        if (currentVersion != null) {
            item = currentVersion.getItem();
        }
        return newItemVersion(item, type);
    }

    /**
     * Produces a new ItemVersion, and a new Item if required, to represent
     * either a new resource or a modification to an existing one.
     *
     * @param item - the existing item identifier if it exists, or null to
     * create a new one
     * @param type - type of the resource, d=directory, f=file
     * @return
     */
    public static ItemVersion newItemVersion(Item item, String type) {
        if (item == null) {
            item = new Item();
            log.warn("Creating a new Item");
            item.setType(type);
            item.setCreateDate(new Date());
        }

        ItemVersion itemVersion = new ItemVersion();
        itemVersion.setModifiedDate(new Date());
        itemVersion.setItem(item);

        if (item.getVersions() == null) {
            List<ItemVersion> list = new ArrayList<>();
            item.setVersions(list);
        }
        item.getVersions().add(itemVersion);

        SessionManager.session().save(item);
        SessionManager.session().save(itemVersion);
        return itemVersion;
    }

    private static boolean isHtml(FileResource rfr) {
        String ct = rfr.getContentType("text/html"); // find if it can produce html
        return "text/html".equals(ct);
    }
}
