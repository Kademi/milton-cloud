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
package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.web.RepositoryFolder;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;

/**
 *
 * @author brad
 */
public class ForumsApp  implements Application {

    @Override
    public String getInstanceId() {
        return "programsAdmin";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if( parent instanceof ForumsAdminFolder) {
            ForumsAdminFolder faf = (ForumsAdminFolder) parent;
            
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof RepositoryFolder) {
            RepositoryFolder repoFolder = (RepositoryFolder) parent;
            System.out.println("found repo folder: " + repoFolder.getName());
            Repository r = repoFolder.getRepository();
            List<Website> list = Website.findByWebsite(r, SessionManager.session());
            System.out.println("websites: " + list.size());
            if( !list.isEmpty()) {
                Website w = list.get(0); // should only ever be 1
                ForumsAdminFolder forumsAdminFolder = new ForumsAdminFolder("forums", repoFolder, w);
                children.add(forumsAdminFolder);
            }            
        }
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
