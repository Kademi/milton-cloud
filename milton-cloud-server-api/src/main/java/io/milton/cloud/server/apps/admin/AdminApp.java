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
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.util.List;

/**
 *
 * @author brad
 */
public class AdminApp implements Application {

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
            if (requestedName.equals("dashboard")) {
                return new TemplatedHtmlPage("dashboard", p, p.getServices(), "admin/dashboard");
            } else if (requestedName.equals("users")) {
                return new UserAdminPage(requestedName, p.getOrganisation(), p, p.getServices());
            } else if (requestedName.equals("websites")) {
                return new WebsitesAdminPage(requestedName, p.getOrganisation(), p, p.getServices());
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
    }

    @Override
    public void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder) {
    }
}
