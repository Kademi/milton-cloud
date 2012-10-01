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

import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;

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
     * Get the title for the application. This should be a simple, one line
     * string
     * 
     * @param organisation
     * @param website
     * @return 
     */
    String getTitle(Organisation organisation, Branch websiteBranch);

    
    /**
     * Return a textual summary of the application and its configuration (if any)
     * for the given org and (optionally) website
     * 
     * @param rootFolder
     * @return 
     */
    String getSummary(Organisation organisation,Branch websiteBranch);


    
}
