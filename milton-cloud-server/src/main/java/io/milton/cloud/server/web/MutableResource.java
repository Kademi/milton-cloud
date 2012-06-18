package io.milton.cloud.server.web;

import io.milton.vfs.db.ItemHistory;
import io.milton.vfs.db.MetaItem;

/**
 * Represents a web resource which can be changed (ie is mutable)
 * 
 * Being mutable is has a hash value
 *
 * @author brad
 */
public interface MutableResource extends SpliffyResource {

    /**
     * Flag which indicates that this resource or its members (if a directory) have changed
     * 
     * @return 
     */
    boolean isDirty();    
           
    Long getEntryHash();
    
    MetaItem getItemVersion();

    /**
     * Called during the save process. Must reset dirty flag!!!
     * 
     * @param newVersion 
     */
    void setItemVersion(MetaItem newVersion);
    
    
    /**
     * The type string, "d" or "f"
     * 
     * @return 
     */
    String getType();

    /**
     * If this resource is currently linked to a DM, return it. Otherwise returns
     * null.
     * 
     * @return 
     */
    ItemHistory getDirectoryMember();
}
