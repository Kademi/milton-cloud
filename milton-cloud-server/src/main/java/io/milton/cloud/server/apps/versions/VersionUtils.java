package io.milton.cloud.server.apps.versions;

import java.util.ArrayList;
import java.util.List;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.ItemVersion;

/**
 *
 * @author brad
 */
public class VersionUtils {
    public static List<AbstractVersionResource> toResources(VersionCollectionResource parent, List<DirectoryMember> dirEntries) {
        List<AbstractVersionResource> list = new ArrayList<>();
        if (dirEntries != null) {
            for (DirectoryMember de : dirEntries) {
                String name = de.getName();
                AbstractVersionResource r = toResource(parent, de);
                list.add(r);
            }
        }
        return list;
    }    

    public static  AbstractVersionResource toResource(VersionCollectionResource parent, DirectoryMember de) {
        ItemVersion itemVersion = de.getMemberItem();
        String type = itemVersion.getItem().getType();
        switch (type) {
            case "d":
                DirectoryVersionResource rdr = new DirectoryVersionResource(parent, de);
                return rdr;
            case "f":
                FileVersionResource rfr = new FileVersionResource(parent, de); 
                return rfr;
            default:
                throw new RuntimeException("Unknown resource type: " + type);
        }
    }    
}
