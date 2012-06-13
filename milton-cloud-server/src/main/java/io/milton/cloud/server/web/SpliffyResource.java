package io.milton.cloud.server.web;

import java.util.List;
import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.db.Profile;
import io.milton.common.Path;
import io.milton.http.AccessControlledResource;
import io.milton.resource.DigestResource;

/**
 * Common interface for all spliffy resources
 *
 * @author brad
 */
public interface SpliffyResource extends DigestResource{
        
    
    /**
     * Get the parent item in the folder hierarchy
     * 
     * @return 
     */
    SpliffyCollectionResource getParent();
    
    /**
     * Convenient access to main service singletons
     * 
     * @return 
     */
    Services getServices();
    
    /**
     * Find whatever entity (user or other) which owns the given resource
     * 
     * @return 
     */
    BaseEntity getOwner();
    
    /**
     * Returns the current user on this request
     * 
     * @return 
     */
    Profile getCurrentUser();

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
}
