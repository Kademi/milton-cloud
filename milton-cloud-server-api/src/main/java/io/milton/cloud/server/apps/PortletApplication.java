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
 * Portlets in a Website can be ordered. To do this add a property to the theme
 * attributes file (eg /theme/theme-attributes.properties) like this:
 * 
 * ordering-dashboardSecondary=App-Test1, Calendars
 * 
 * Which would put the App-Test1 portlet first, then Calendars, then any other portlet
 * in the default order
 * 
 * 
 *
 * @author brad
 */
public interface PortletApplication extends Application{
    /**
     * Inside the HEAD tag, following all template declarations
     */
    public static final String PORTLET_SECTION_HEADER = "header";
    
    public static final String PORTLET_SECTION_END_OF_PAGE = "endOfPage";
    
    /**
     * Inside the navigation section
     */
    public static final String PORTLET_SECTION_NAV_HEADER = "navHeader";
    /**
     * Renders at the top of the user dashboard page
     */
    public static final String PORTLET_SECTION_DASH_MESSAGES = "dashboardMessages";
    
    /**
     * The main section of the user dashboard
     */
    public static final String PORTLET_SECTION_DASH_PRIMARY = "dashboardPrimary";
    
    /**
     * The secondary section of the user dashboard
     */
    public static final String PORTLET_SECTION_DASH_SECONDARY = "dashboardSecondary";
    
    /**
     * For email triggers, shows additional actions
     */
    public static final String PORTLET_SECTION_TRIGGER_ACTIONS = "triggerActions";
    
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
