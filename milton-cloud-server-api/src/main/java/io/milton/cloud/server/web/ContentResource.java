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

import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import java.io.IOException;

/**
 * Base interface for all classes which represent items from a content repository
 *
 * @author brad
 */
public interface ContentResource extends CommonResource{
    
    /**
     * Get the "fingerprint" of the current version of this resource
     * 
     * @return 
     */
    String getHash();
    
    /**
     * Used for reverting to an earlier version
     * 
     * @param s 
     */
    void setHash(String s);

    Profile getModifiedBy();
    
    /**
     * Save changes to the database. This will usually invoke save on a parent, 
     * except for the branch which will set the commit record
     */
    void save() throws IOException;  
    
    /**
     * Get the branch that contains this content
     * 
     * @return 
     */
    Branch getBranch();
        
}
