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

import io.milton.vfs.content.ContentSession.DirectoryNode;

/**
 * Implemented by content resources, which dont save themselves, but delegate
 * to a repository session
 *
 * @author brad
 */
public interface ContentDirectoryResource extends CommonCollectionResource, ContentResource{
    /**
     * Either save this content session, or delegate to a parent who can
     * 
     */
    void save();    
    
    /**
     * Get the underlying directory node for this instance
     * 
     * @return 
     */
    DirectoryNode getDirectoryNode();

    void onAddedChild(AbstractContentResource aThis);

    void onRemovedChild(AbstractContentResource aThis);
}
