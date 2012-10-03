/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.role;

import edu.emory.mathcs.backport.java.util.Collections;
import io.milton.cloud.server.web.CommonResource;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Repository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A user is assigned roles by being put into a group. However, the group
 * membership only applies within some context, and so any roles for that
 * user only apply within that context as well.
 * 
 * For example, Sally is in the Sales Rep group for the Southern Region. That group
 * gives Sally the right to edit user accounts within the Southern Region, but it
 * does not allow her any rights in the Northern Region. So when Sally accesses
 * some resource we will look through her groups finding roles. For each role we'll
 * check if it applies to the resource type being accessed, and if so if that
 * resource is within the context of the group membership. If so then the priviledges
 * conveyed will be returned and used to determine whether to allow the request
 *
 * @author brad
 */
public interface Role {

    public static Set<AccessControlledResource.Priviledge> READ = Collections.unmodifiableSet( new HashSet(Arrays.asList(AccessControlledResource.Priviledge.READ)) );
    
    public static Set<AccessControlledResource.Priviledge> READ_WRITE = Collections.unmodifiableSet( new HashSet(Arrays.asList(AccessControlledResource.Priviledge.READ, AccessControlledResource.Priviledge.WRITE)) );
    
    
    /**
     * The name of this role. This will be persisted so should never change
     * 
     * @return 
     */
    String getName();


    /**
     * Called for roles which have been assigned to a user. This check is to find
     * out if the organisation in which the user has been given the role contains
     * the given resource
     * 
     * Check if this role applies to the given resource, when assigned to a user
     * within the given withinOrg.
     * 
     * Returns true if:
     *  - the resource is of a type which this role is interested in
     *  - the resource is contained within the withinOrg
     * 
     * Returning false does not necessarily reject the request, it merely doesnt
     * accept it. Some other Role may choose to accept it.
     * 
     * @param resource - the resource which is being checked
     * @param withinOrg - this is the organisation in which the group membership of the current user applies
     * @param requesting - the group which has the role being checked. Some roles might use the group as a factor
     * @return 
     */
    boolean appliesTo(CommonResource resource, Organisation withinOrg, Group requesting);
    
    /**
     * For a user who does have this role on some resource (as determined by appliesTo)
     * 
     * @return 
     */
    Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g);

    /**
     * Determine if this role applies to the given repository
     * 
     * @param resource
     * @param applicableRepo
     * @param g
     * @return 
     */
    boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g);

    /**
     * Get priviledges conferred by this role on the repository
     * 
     * @param resource
     * @param applicableRepo
     * @param g
     * @return 
     */
    Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g);
}
