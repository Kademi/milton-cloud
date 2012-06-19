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

import io.milton.cloud.server.web.ResourceList;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.List;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.MenuItem;

/**
 * Defines an extensibility mechanism for Spliffy.
 * 
 * At its simplest this is a means to locate resources defined by the application
 * which exist
 *
 * @author brad
 */
public interface Application {
    
    /**
     * Indentifies this instance of this app in a way which persistent across server
     * restarts. This will be used to locate properties
     * 
     * @return 
     */
    String getInstanceId();
    
    /**
     * Called on the application when the app starts
     * 
     */
    void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception;
    
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

    /**
     * Add instances of resources which should be browseable from webdav clients
     * 
     * @param parent
     * @param children 
     */
    void addBrowseablePages(CollectionResource parent, ResourceList children);
    
    /**
     * Causes the application to release all resources. It should be restartable
     */
    void shutDown();

   
    /**
     * Called when no configuration file exists. Populate the given object with
     * default values, this will be stored to file so the administrator can
     * review and edit as required
     * 
     * @param config 
     */
    void initDefaultProperties(AppConfig config);

    /**
     * Add menu items if appropriate
     * 
     * @param list
     * @param r
     * @param user
     * @param rootFolder 
     */
    void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder);


    
}
