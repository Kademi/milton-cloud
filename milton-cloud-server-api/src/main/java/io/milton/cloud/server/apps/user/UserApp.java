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
import io.milton.cloud.server.db.utils.UserDao;
import io.milton.cloud.server.web.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class UserApp implements Application{

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserApp.class);
    
    public static String USERS_FOLDER_NAME = "users";
    
    public static PrincipalResource findEntity(Profile u, RootFolder rootFolder) throws NotAuthorizedException, BadRequestException {
        System.out.println("findEntity");
        Resource r = rootFolder.child(USERS_FOLDER_NAME);
        if( r instanceof UsersFolder) {
            UsersFolder uf = (UsersFolder) r;
            Resource p = uf.child(u.getName());
            if( p instanceof PrincipalResource) {
                PrincipalResource pr = (PrincipalResource) p;
                return pr;
            } else if( p != null ) {
                log.warn("Found a resource which is not a principalResource: " + p.getClass());
            }
        } else {
            log.warn("users folder not found: " + USERS_FOLDER_NAME + " in " + rootFolder.getClass());
        }
        return null;
    }
    
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
            UserDao userDao = _(UserDao.class);
            Profile p = userDao.findProfile(requestedName, uf.getOrganisation(), SessionManager.session());
            if( p != null ) {
                return new UserResource(uf, p);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        log.info("addBrowseablePages: " + parent.getClass());
        if( parent instanceof RootFolder) {
            RootFolder wrf = (RootFolder) parent;
            log.info("adding users folder");
            children.add(new UsersFolder(wrf, USERS_FOLDER_NAME));
        } else if( parent instanceof OrganisationFolder) {
            OrganisationFolder organisationFolder = (OrganisationFolder) parent;
            children.add(new UsersFolder(organisationFolder, USERS_FOLDER_NAME));
        }
    }
    
}
