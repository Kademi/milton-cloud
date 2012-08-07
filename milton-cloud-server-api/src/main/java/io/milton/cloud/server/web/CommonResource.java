package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;
import io.milton.common.Path;
import io.milton.resource.DigestResource;

/**
 * Common interface for all spliffy resources
 *
 * @author brad
 */
public interface CommonResource extends DigestResource{
        
    
    /**
     * Get the parent item in the folder hierarchy
     * 
     * @return 
     */
    CommonCollectionResource getParent();
    
   
    

    /**
     * Get the organisation which most directly contains this resource
     * 
     * @return 
     */
    Organisation getOrganisation();
         
    
    /*
     * A simple "is" test to see if this resource represents something in particular.
     * 
     * Exactly what values should satisfy this test for different resources varies,
     * but should generally be intuitive. Eg UserResource.is("user") = true, etc
     * 
     */
    boolean is(String type);
    
    /**
     * The absolute path to this resource
     * 
     * @return 
     */
    Path getPath();
    
    /**
     * Used for theming, is a way for the resource to indicate if it is in a secure
     * or public realm
     * 
     * @return 
     */
    boolean isPublic();    
}
