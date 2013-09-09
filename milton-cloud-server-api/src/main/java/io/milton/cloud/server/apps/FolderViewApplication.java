/*
 * Copyright 2013 McEvoy Software Ltd.
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

import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Display a page for a folder
 *
 * @author brad
 */
public interface FolderViewApplication extends Application {
    /**
     * Render the view of the folder
     * 
     * @param out
     * @param params
     * @param contentType
     * @throws IOException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     * @throws NotFoundException 
     */
    void renderPage(ContentDirectoryResource folder, OutputStream out, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException;

    /**
     * Return true if this app can handle the given type of root. Typically AdminApp
     * will handle stuff inside the admin console, while some front-end app
     * will hand stuff inside WebsiteRootFolder
     * 
     * @param rf
     * @return 
     */
    boolean supports(RootFolder rf);
}
