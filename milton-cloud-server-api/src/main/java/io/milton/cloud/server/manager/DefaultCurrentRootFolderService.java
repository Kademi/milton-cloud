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
package io.milton.cloud.server.manager;

import io.milton.cloud.server.web.RootFolder;
import io.milton.http.HttpManager;
import io.milton.http.Request;

/**
 * Stores the root folder in a request attribute. Is null safe, ie does nothing
 * if there is no request
 *
 * @author brad
 */
public class DefaultCurrentRootFolderService implements CurrentRootFolderService{

    public static String ROOT_FOLDER_NAME = "_spliffy_root_folder";
    
    @Override
    public RootFolder getRootFolder() {
        Request req = HttpManager.request();
        if( req == null ) {
            return null;
        }
        return (RootFolder) req.getAttributes().get(ROOT_FOLDER_NAME);
    }

    @Override
    public void setRootFolder(RootFolder r) {
        Request req = HttpManager.request();
        if( req == null ) {
            return;
        }
        req.getAttributes().put(ROOT_FOLDER_NAME, r);
    }
    
}
