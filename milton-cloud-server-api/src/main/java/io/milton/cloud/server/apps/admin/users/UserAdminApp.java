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
package io.milton.cloud.server.apps.admin.users;

import io.milton.cloud.server.apps.admin.UserAdminPage;
import io.milton.cloud.server.web.templating.MenuItem;
import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class UserAdminApp implements Application {

    private SpliffyResourceFactory resourceFactory;

    @Override
    public String getInstanceId() {
        return "manageUsers";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.resourceFactory = resourceFactory;
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationRootFolder) {
            if (requestedName.equals("manageUsers")) {
                OrganisationRootFolder orgFolder = (OrganisationRootFolder) parent;
                return new UserAdminPage(requestedName,orgFolder.getOrganisation(), (CommonCollectionResource) parent, resourceFactory.getServices());
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
        if (rootFolder instanceof OrganisationRootFolder) {
            MenuItem m = new MenuItem();
            m.setText("Manage users");
            m.setHref("/manageUsers/");
            m.setId("userAdmin");
            list.add(m);
        }
    }
}
