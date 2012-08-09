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

import io.milton.cloud.server.web.RootFolder;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.Writer;
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
public interface PortletApplication extends Application{
    
    public static final String PORTLET_SECTION_HEADER = "header";
    public static final String PORTLET_SECTION_DASH_MESSAGES = "dashboardMessages";
    public static final String PORTLET_SECTION_DASH_PRIMARY = "dashboardPrimary";
    public static final String PORTLET_SECTION_DASH_SECONDARY = "dashboardSecondary";
    
    /**
     * 
     * @param portletSection - the name of the section being rendered
     * @param currentUser - the current user
     * @param rootFolder - the root folder we're operating under
     * @param context - the current velocity context
     * @param writer - to write content to
     */
    void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException;
}
