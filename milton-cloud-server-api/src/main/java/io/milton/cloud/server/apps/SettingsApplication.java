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
package io.milton.cloud.server.apps;

import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.RootFolder;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 * An Application which can render portlets. These are sections that can
 * be displayed in other pages
 * 
 * See constants in this interface for standard portlet section names, but 
 * templates are free to choose their own names
 *
 * @author brad
 */
public interface SettingsApplication extends Application{
        
    /**
     * Generate the settings html fragment to be displayed on the manage settings
     * page. This will be inside a form with the action set and method=POST, and
     * submit and cancel buttons are displayed for you
     * 
     * The from will be posted to the ManageApplications page and then delegated to
     * the application
     * 
     * @param currentUser - the current user
     * @param rootFolder - the root folder we're operating under
     * @param context - the current velocity context
     * @param writer - to write content to
     */
    void renderSettings(Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException;
    
    /**
     * Process a form POST 
     * 
     * The transaction will be commited by the calling code if jsonResult.status == true
     * 
     * Any validation errors should be returned in the jsonResult object
     * 
     * @param parameters
     * @param files
     * @return
     * @throws BadRequestException
     * @throws NotAuthorizedException
     * @throws ConflictException 
     */
    JsonResult processForm(Map<String,String> parameters, Map<String,FileItem> files, Organisation org, Website website) throws BadRequestException, NotAuthorizedException, ConflictException;
}
