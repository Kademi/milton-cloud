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
package io.milton.cloud.server.apps.versions;

import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.web.templating.MenuItem;
import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.UserResource;

/**
 *
 * @author brad
 */
public class VersionsApp implements Application {

    private Services services;

    @Override
    public String getInstanceId() {
        return "versions";
    }
    
    
    
    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.services = resourceFactory.getServices();
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        return null;
    }

    @Override
    public void shutDown() {
        
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, List<Resource> children) {
        if( parent instanceof UserResource) {
            UserResource ur = (UserResource) parent;
            BaseEntity baseEntity = ur.getOwner();
            VersionsRootFolder f = new VersionsRootFolder(ur, baseEntity, services);
            children.add(f);
        }
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
    
    }

    @Override
    public void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder) {
        
    }
    
    
}
