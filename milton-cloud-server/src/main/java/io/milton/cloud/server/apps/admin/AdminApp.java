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
package io.milton.cloud.server.apps.admin;

import edu.emory.mathcs.backport.java.util.Collections;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationsFolder;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.Set;


/**
 *
 * @author brad
 */
public class AdminApp implements MenuApplication {

    @Override
    public String getInstanceId() {
        return "admin";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        resourceFactory.getSecurityManager().add(new AdminRole());
        resourceFactory.getSecurityManager().add(new UserAdminRole());
    }

    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Provides most admin console functionality, such as managing users, groups, websites, etc";
    }
    
    

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationFolder) {
            CommonCollectionResource p = (CommonCollectionResource) parent;
            switch (requestedName) {
                case "manageUsers":
                    MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuUsers");
                    return new ManageUsersFolder(requestedName, p.getOrganisation(), p);
                case "groups":
                    MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuGroups");
                    return new ManageGroupsPage(requestedName, p.getOrganisation(), p);
                case "manageWebsites":
                    MenuItem.setActiveIds("menuDashboard", "menuWebsiteManager", "menuWebsites");
                    return new ManageWebsitesFolder(requestedName, p.getOrganisation(), p);
                case "manageApps":
                    MenuItem.setActiveIds("menuDashboard", "menuWebsiteManager", "manageApps");
                    return new ManageAppsPage(requestedName, p.getOrganisation(), p, null);
                    
            }
        } else if (parent instanceof OrganisationsFolder) {
            OrganisationsFolder orgsFolder = (OrganisationsFolder) parent;
            if (requestedName.equals("manage")) {
                MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuOrgs");
                return new ManageOrgsPage(requestedName, orgsFolder.getOrganisation(), orgsFolder);
            }
        } else if( parent instanceof ManageWebsiteFolder )  {
            if( requestedName.equals("apps")) {
                CommonCollectionResource p = (CommonCollectionResource) parent;
                ManageWebsiteFolder mwp = (ManageWebsiteFolder) parent;
                return new ManageAppsPage(requestedName, p.getOrganisation(), p, mwp.getWebsite());
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }

    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
        if(parentOrg == null ) {
            return ;
        }
        Path parentPath = parentOrg.getPath();
        switch (parentId) {
            case "menuRoot":
                parent.getOrCreate("menuDashboard", "My Dashboard", parentPath).setOrdering(10);
                break;
            case "menuDashboard":
                parent.getOrCreate("menuGroupsUsers", "Groups &amp; users").setOrdering(20);
                parent.getOrCreate("menuWebsiteManager", "Website manager").setOrdering(30);
                break;
            case "menuGroupsUsers":
                parent.getOrCreate("menuUsers", "Manage users", parentPath.child("manageUsers")).setOrdering(10);
                parent.getOrCreate("menuGroups", "Manage groups", parentPath.child("groups")).setOrdering(20);
                Path p = parentOrg.getPath().child("organisations").child("manage");
                parent.getOrCreate("menuOrgs", "Manage Business units", p).setOrdering(30);
                break;
            case "menuWebsiteManager":
                parent.getOrCreate("menuWebsites", "Setup your websites", parentPath.child("manageWebsites")).setOrdering(10);
                parent.getOrCreate("menuThemes", "Templates &amp; themes", parentPath.child("themes")).setOrdering(20);
                parent.getOrCreate("menuApps", "Applications", parentPath.child("manageApps")).setOrdering(30);
                break;
        }
    }
    
    public class AdminRole implements Role {

        @Override
        public String getName() {
            return "Administrator";
        }

  
        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            Organisation resourceOrg = resource.getOrganisation();
            boolean  b = resourceOrg.isWithin(withinOrg); 
            System.out.println("appliesTo: " + resourceOrg.getName() + " - " + withinOrg.getName() + " = " + b);
            return b;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Collections.singleton(Priviledge.ALL);
        }
        
    }
    
    
    public class UserAdminRole implements Role {

        @Override
        public String getName() {
            return "User Administrator";
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if( resource instanceof UserResource) {
                UserResource ur = (UserResource) resource;
                return ur.getOrganisation().isWithin(withinOrg);
            }
            return false;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Role.READ_WRITE;
        }

    }      
}
