package io.milton.cloud.server.web;

import io.milton.vfs.db.Organisation;
import io.milton.common.Path;
import io.milton.http.Request;
import io.milton.resource.AccessControlledResource.Priviledge;
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

    /**
     * Determine what priviledge is required for doing a POST to this resource
     * with the given request.
     * 
     * Note that this can vary a lot. For example, posting a comment might only
     * require READ_CONTENT, but a POST which updates a page will require WRITE_CONTENT
     * 
     * @param request
     * @return 
     */
    Priviledge getRequiredPostPriviledge(Request request);
    
    /**
     * Find the closest parent resource (or this) which satisfies the is(..)
     * function for the given type
     * 
     * For example page.closest("branch") would return a BranchFolder because
     * BranchFolder.is returns true for "branch"
     * 
     * @param type
     * @return 
     */
    CommonResource closest(String type);
}
