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
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.config.HttpManagerBuilder;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;

import io.milton.http.http11.auth.CookieAuthenticationHandler;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.List;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class UserApp implements Application, ChildPageApplication, BrowsableApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserApp.class);
    public static String USERS_FOLDER_NAME = "users";

    public static String getPasswordResetBase(Website w) {
        String domainName = w.getDomainName();
        if( domainName == null ) {
            domainName = w.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
        }
        String s = _(Formatter.class).getPortString();
        domainName = domainName + s;
        
        return "http://" + domainName + "/do-reset";
    }        
    
    public static String getPasswordResetHref(Website w) {
        return getPasswordResetBase(w) + "?token=$page.passwordReset.token";
    }    
    
    public static PrincipalResource findEntity(Profile u, RootFolder rootFolder) throws NotAuthorizedException, BadRequestException {
        log.info("findEntity");
        Resource r = rootFolder.child(USERS_FOLDER_NAME);
        if( r == null ) {
            r = new UsersFolder(rootFolder, USERS_FOLDER_NAME);
            List children = rootFolder.getChildren();
            children.add(r);
        }
        if (r instanceof UsersFolder) {
            UsersFolder uf = (UsersFolder) r;
            Resource p = uf.findUserResource(u);
            if (p instanceof PrincipalResource) {
                PrincipalResource pr = (PrincipalResource) p;
                return pr;
            } else if (p != null) {
                log.warn("Found a resource which is not a principalResource: " + p.getClass());
                return null;
            } else {
                log.warn("Could not get a child resource from users folder");
                return null;
            }
        } else {
            if (r == null) {
                log.warn("users folder not found: " + USERS_FOLDER_NAME + " in " + rootFolder.getClass() + " - " + rootFolder.getName() + "  hash: " + rootFolder.hashCode());
            } else {
                log.warn("users folder not found, incorrect type: " + r.getClass() + " expected: " + UsersFolder.class);
            }
            return null;
        }        
    }
    private SpliffySecurityManager securityManager;
    private CookieAuthenticationHandler cookieAuthenticationHandler; // Needed for logging users in on password reset

    @Override
    public String getInstanceId() {
        return "userApp";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Users";
    }
        
    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Core application which provides user functions, such as dashboard, login and profile pages";
    }
    
    

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        securityManager = resourceFactory.getSecurityManager();
        HttpManagerBuilder httpManagerBuilder = config.getContext().get(HttpManagerBuilder.class);
        cookieAuthenticationHandler = httpManagerBuilder.getCookieAuthenticationHandler();
        log.info("init UserApp: cookieAuthenticationHandler: " + cookieAuthenticationHandler);
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof UsersFolder) {
            UsersFolder uf = (UsersFolder) parent;
            Profile p = Profile.find(requestedName, SessionManager.session());
            if (p != null) {
                return new UserResource(uf, p);
            }
        } else if (parent instanceof RootFolder) {
            RootFolder rf = (RootFolder) parent;
            switch (requestedName) {
                case "profile":
                    MenuItem.setActiveIds("menuDashboard");
                    return new ProfilePage(requestedName, rf);
            }
        } else if( parent instanceof UserResource) {
            UserResource user = (UserResource) parent;
            if( requestedName.equals("public")) {
                return new PublicProfilePage(requestedName, user.getThisUser(), user);
            }
        }
        if (parent instanceof RootFolder) {            
            RootFolder rf = (RootFolder) parent;
            switch (requestedName) {
                case "login.html":
                    return new LoginPage(rf);
                case "password-reset":
                    return new PasswordResetPage(requestedName, rf);
                case "do-reset":
                    return new DoPasswordResetPage(requestedName, rf, cookieAuthenticationHandler);
            }
        }
        return null;
    }


    
    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof RootFolder) {
            RootFolder wrf = (RootFolder) parent;
            children.add(new UsersFolder(wrf, USERS_FOLDER_NAME));
        } else if (parent instanceof OrganisationFolder) {
            OrganisationFolder organisationFolder = (OrganisationFolder) parent;
            children.add(new UsersFolder(organisationFolder, USERS_FOLDER_NAME));
        }
    }

}
