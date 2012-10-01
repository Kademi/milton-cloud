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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.Resource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;

/**
 *
 * @author brad
 */
public class UserDashboardApp implements Application, MenuApplication, ChildPageApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserDashboardApp.class);
    public static String USERS_FOLDER_NAME = "users";

    @Override
    public String getInstanceId() {
        return "userDashboardApp";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "User Dashboard";
    }
        
    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Gives each user a dashboard page which can display all sorts of things";
    }
    
    

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            switch (requestedName) {
                case "dashboard":
                    MenuItem.setActiveIds("menuDashboard");
                    return new DashboardPage(requestedName, wrf);                    
            }
        }
        return null;
    }


    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        switch (parentId) {
            case "menuRoot":
                if (parent.getRootFolder() instanceof WebsiteRootFolder) {
                    if (parent.getUser() != null) {
                        parent.getOrCreate("menuDashboard", "Dashboard", "/dashboard").setOrdering(10);
                    }
                }
        }
    }
}
