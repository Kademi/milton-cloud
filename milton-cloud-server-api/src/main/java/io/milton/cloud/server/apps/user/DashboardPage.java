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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.TemplatedHtmlPage;

/**
 * Dashboard for the user. Most functions will be provided by PortletApplications
 * which will be invoked from the template using the PortletsDirective
 * 
 * Eg:
 * #portlets("messages") 
 * , which will render portlets for the messages section of the page
 * 
 * Standard sections are intended to be:
 * messages - brief list of messages at top of page
 * primary - this is the main section of the page, with about 70% width
 * secondary - this is a narrowed section of the page, possible lower down for small screen clients
 * 
 *
 * @author brad
 */
public class DashboardPage extends TemplatedHtmlPage {

    public DashboardPage(String name, CommonCollectionResource parent) {
        super(name, parent, "user/dashboard", "Dashboard");
        setForceLogin(true);
    }

    @Override
    public boolean is(String type) {
        if( type.equals("dashboard")) {
            return true;
        }
        return super.is(type);
    }

    

}
