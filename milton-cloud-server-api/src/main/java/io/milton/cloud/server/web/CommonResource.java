package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import java.util.List;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.common.Path;
import io.milton.resource.AccessControlledResource;
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
     * Find whatever entity (user or other) which owns the given resource
     * 
     * @return 
     */
    BaseEntity getOwner();
    

    /**
     * Get the organisation which most directly contains this resource
     * 
     * @return 
     */
    Organisation getOrganisation();
        
    /**
     * Add whatever permissions are defined on this resource for the given user
     * 
     * @param list
     * @param user 
     */
    void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user);    
    
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
