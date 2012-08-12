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
package io.milton.cloud.server.apps;

import io.milton.cloud.server.web.RootFolder;
import io.milton.resource.Resource;

/**
 * A specialisation of Application which allows an application to provide a specific
 * child resource (by name) to any parent resource. Note that parent resources
 * must delegate the initialisation of their child collections to the ApplicationManager
 * 
 * Typically, an implementation will check the type of the given parent, and then
 * if it is interested in that type it will check to see if the name matches
 * some. Eg:
 * 
 *         if (parent instanceof GroupInWebsiteFolder) {
 *           GroupInWebsiteFolder wrf = (GroupInWebsiteFolder) parent;
 *           if (requestedName.equals(signupPageName)) {
 *               return new GroupRegistrationPage(requestedName, wrf);
 *           }
 *       }
 *       return null;
 *
 * @author brad
 */
public interface ChildPageApplication extends Application {
    /**
     * Return a resource for the given parent of the given name if this
     * application defines one, and if it is not returned from addBrowseablePages. Or return null otherwise
     * 
     * Usually an Application will check the type of the parent and only
     * return a resource if the type is something its handling, like a UserResource
     * 
     * @param parent
     * @param requestedName
     * @return 
     */
    Resource getPage(Resource parent, String requestedName);
}
