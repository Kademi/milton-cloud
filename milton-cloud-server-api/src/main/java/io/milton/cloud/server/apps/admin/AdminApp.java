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

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationsFolder;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplateRenderer;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

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
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationFolder) {
            CommonCollectionResource p = (CommonCollectionResource) parent;
            switch (requestedName) {
                case "dashboard":
                    MenuItem.setActiveId("menuDashboard");
                    return new TemplatedHtmlPage("dashboard", p, p.getServices(), "admin/dashboard", "Admin dashboard");
                case "manageUsers":
                    MenuItem.setActiveIds("menuManagement", "menuGroupsUsers", "menuUsers");
                    return new UserAdminPage(requestedName, p.getOrganisation(), p, p.getServices());
                case "groups":
                    MenuItem.setActiveIds("menuManagement", "menuGroupsUsers", "menuGroups");
                    return new GroupsAdminPage(requestedName, p.getOrganisation(), p, p.getServices());
                case "websites":
                    MenuItem.setActiveIds("menuManagement", "menuWebsiteManager", "menuWebsites");
                    return new WebsitesAdminPage(requestedName, p.getOrganisation(), p, p.getServices());
            }
        } else if (parent instanceof OrganisationsFolder) {
            OrganisationsFolder orgsFolder = (OrganisationsFolder) parent;
            if (requestedName.equals("manage")) {
                MenuItem.setActiveIds("menuManagement", "menuGroupsUsers", "menuOrgs");
                return new OrgsAdminPage(requestedName, orgsFolder.getOrganisation(), orgsFolder, orgsFolder.getServices());
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
        Path parentPath = parentOrg.getPath();
        switch (parentId) {
            case "menuRoot":
                parent.getOrCreate("menuManagement", "Content management").setOrdering(20);
                parent.getOrCreate("menuDashboard", "My Dashboard", parentPath.child("dashboard")).setOrdering(10);
                break;
            case "menuManagement":
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
                parent.getOrCreate("menuWebsites", "Setup your websites", parentPath.child("websites")).setOrdering(10);
                parent.getOrCreate("menuThemes", "Templates &amp; themes", parentPath.child("themes")).setOrdering(20);
                break;
        }
    }
}
