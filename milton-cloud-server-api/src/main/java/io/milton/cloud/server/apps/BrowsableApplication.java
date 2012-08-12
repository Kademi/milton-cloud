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

/**
 * A specialisation of Application which allows an application to inject browsable
 * resources into the folder/file hierarchy. This means that a user with a webdav client
 * can browse through the resources.
 *
 * @author brad
 */
public interface BrowsableApplication extends Application {
    /**
     * Add instances of resources which should be browseable from webdav clients.
     * 
     * The parent folder must delegate child initialisation to the ApplicationManager
     * 
     * @param parent
     * @param children 
     */
    void addBrowseablePages(CollectionResource parent, ResourceList children);
}
