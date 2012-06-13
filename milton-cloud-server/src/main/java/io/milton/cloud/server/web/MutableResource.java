package io.milton.cloud.server.web;

import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.ItemVersion;

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
    
    ItemVersion getItemVersion();

    /**
     * Called during the save process. Must reset dirty flag!!!
     * 
     * @param newVersion 
     */
    void setItemVersion(ItemVersion newVersion);
    
    
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
    DirectoryMember getDirectoryMember();
}
