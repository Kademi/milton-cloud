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
package io.milton.cloud.server.apps.admin.orgs;

import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.web.ResourceList;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class OrgAdminApp implements Application{

    @Override
    public String getInstanceId() {
        return "orgAdmin";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
