/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.util.Map;

/**
 *
 * @author brad
 */
public interface RootFolder extends CommonCollectionResource {
    /**
     * Find the user,group or organisation within this folder with the given name
     * 
     * @param name
     * @return 
     */
    PrincipalResource findEntity(Profile user) throws NotAuthorizedException, BadRequestException;
    
    @Override
    Organisation getOrganisation();
    
    /**
     * Just another map of objects for the request
     * 
     * @return 
     */
    Map<String,Object> getAttributes();

    /**
     * Identifier for this root folder. This is used for building urls which identify
     * the root folder in other contexts (Eg administration)
     * 
     * @return 
     */
    String getId();
    
    /**
     * An email address to use when sending emails from this logical entity
     * 
     * @return 
     */
    String getEmailAddress();

    /**
     * Get an actual domain name that can be used to locate this RootFolder via
     * the ResourceFactory
     * 
     * @return 
     */
    String getDomainName();

}
