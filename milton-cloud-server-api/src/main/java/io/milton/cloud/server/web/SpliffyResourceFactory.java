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
package io.milton.cloud.server.web;

import io.milton.cloud.server.db.utils.UserDao;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.manager.DefaultCurrentRootFolderService;
import io.milton.common.Path;
import io.milton.event.EventManager;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class SpliffyResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffyResourceFactory.class);

    public static RootFolder getRootFolder() {
        if (HttpManager.request() != null) {
            return (RootFolder) HttpManager.request().getAttributes().get(DefaultCurrentRootFolderService.ROOT_FOLDER_NAME);
        } else {
            return null;
        }
    }
    private final UserDao userDao;
    private final SpliffySecurityManager securityManager;
    private final ApplicationManager applicationManager;
    private final EventManager eventManager;
    private final SessionManager sessionManager;
    private final CurrentRootFolderService currentRootFolderService;
    
    public SpliffyResourceFactory(UserDao userDao, SpliffySecurityManager securityManager, ApplicationManager applicationManager, EventManager eventManager, SessionManager sessionManager, CurrentRootFolderService currentRootFolderService) {
        this.userDao = userDao;
        this.securityManager = securityManager;
        this.applicationManager = applicationManager;
        this.eventManager = eventManager;
        this.sessionManager = sessionManager;
        this.currentRootFolderService = currentRootFolderService;
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        Path path = Path.path(sPath);
        Resource r = find(host, path);
        if( r == null && sPath.endsWith(".new") ) {
            // Not found, but a html page is requested. If the parent exists and is a collection
            // then we'll instantiate a placeholder page which will allow new pages to be created
            Resource rParent = find(host, path.getParent());
            if( rParent instanceof ContentDirectoryResource) {
                ContentDirectoryResource parentContentDir = (ContentDirectoryResource) rParent;
                return new NewPageResource(parentContentDir, path.getName());
            }
        }
        if( r != null ) {
            log.info("Found a resource: " + r.getClass());
        }
        return r;
    }

    private Resource find(String host, Path p) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        if (p.isRoot()) {
            Resource rootFolder = currentRootFolderService.getRootFolder();
            if (rootFolder == null) {
                rootFolder = applicationManager.getPage(null, host);
                if( rootFolder instanceof RootFolder ) {
                    currentRootFolderService.setRootFolder((RootFolder)rootFolder);
                }
                HttpManager.request().getAttributes().put("_spliffy_root_folder", rootFolder);
            }
            return rootFolder;
        } else {
            Resource rParent = find(host, p.getParent());
            if (rParent == null) {
                return null;
            } else {
                if (rParent instanceof CollectionResource) {
                    CollectionResource parent = (CollectionResource) rParent;
                    return parent.child(p.getName());
                } else {
                    return null;
                }
            }
        }
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public SpliffySecurityManager getSecurityManager() {
        return securityManager;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
       
}
