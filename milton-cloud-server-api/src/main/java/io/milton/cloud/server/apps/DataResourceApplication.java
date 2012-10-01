/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.RootFolder;

/**
 * Represents a type of application which knows how to wrap certain data objects
 * (ie non web enabled) and produce resource objects
 * 
 * These are resources which wrap DataNodes, such as FileResource and DirectoryResource,
 * or a Branch, etc
 *
 * @author brad
 */
public interface DataResourceApplication extends Application {

    /**
     * Instantiate a new resource ONLY if the parent and data node meet the rules
     * of what this creator is interested in.
     * 
     * For example, if a creator wanted to ensure all folder resources within
     * a folder called programs were a certain type, it would only return non-null
     * if the parent had name "programs" and the dataNode was of type DirectoryNode
     * 
     * 
     * @param sourceObject - the data which we need a resource to wrap. For repository content this will be a DataNode
     * @param parent - will be a repository folder, for branches, or otherwise will be a ContentDirectoryResource
     * @param rf - the root folder of the current request. Generally a WebsiteRootFolder indicates rendering mode is required, otherwise administation
     * @return 
     */
    ContentResource instantiateResource(Object sourceObject, CommonCollectionResource parent, RootFolder rf);

    
}
