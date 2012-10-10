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
package io.milton.cloud.server.web;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.PutableResource;
import io.milton.vfs.data.DataSession.DirectoryNode;
import java.util.List;

/**
 * Implemented by content resources, which dont save themselves, but delegate
 * to a repository session
 *
 * @author brad
 */
public interface ContentDirectoryResource extends CommonCollectionResource, ContentResource, MakeCollectionableResource, PutableResource {
    
    /**
     * Get the underlying directory node for this instance
     * 
     * @return 
     */
    DirectoryNode getDirectoryNode();

    void onAddedChild(AbstractContentResource aThis);

    void onRemovedChild(AbstractContentResource aThis);
    
    List<ContentDirectoryResource> getSubFolders() throws NotAuthorizedException, BadRequestException;
    
    List<ContentResource> getFiles() throws NotAuthorizedException, BadRequestException;
    
    FileResource getOrCreateFile(String name) throws NotAuthorizedException, BadRequestException;
            
    /**
     * Locate teh given directory if it exists, and if it doesnt create it if
     * autoCreate is true
     * 
     * @param name
     * @param autoCreate
     * @return
     * @throws NotAuthorizedException
     * @throws NotAuthorizedException
     * @throws BadRequestException 
     */
    DirectoryResource getOrCreateDirectory(String name, boolean autoCreate) throws NotAuthorizedException, NotAuthorizedException, BadRequestException; 
}
