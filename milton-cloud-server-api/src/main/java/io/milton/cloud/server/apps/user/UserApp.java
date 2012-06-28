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
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.UserResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class UserApp implements Application{

    @Override
    public String getInstanceId() {
        return "userApp";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if( parent instanceof UsersFolder) {
            UsersFolder uf = (UsersFolder) parent;
            Profile p = Profile.find(uf.getOrganisation(), requestedName, SessionManager.session());
            if( p != null ) {
                return new UserResource(uf, p, uf.getServices().getApplicationManager());
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if( parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            children.add(new UsersFolder(wrf, "users"));
        } else if( parent instanceof OrganisationFolder) {
            OrganisationFolder organisationFolder = (OrganisationFolder) parent;
            children.add(new UsersFolder(organisationFolder, "users"));
        }
    }
    
}
