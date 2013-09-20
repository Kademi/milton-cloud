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
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.sync.DirectoryHashResource;
import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.event.EventManager;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;
import java.io.File;
import javax.servlet.ServletContext;

import io.milton.servlet.StaticResource;
import io.milton.servlet.UrlResource;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * See DefaultCurrentRootFolderService for notes on domain name resolution
 *
 *
 * @author brad
 */
public class SpliffyResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffyResourceFactory.class);
    public static final String ROOTS_SYS_PROP_NAME = "template.file.roots"; // same as for HtmlTemplater
    private final UserDao userDao;
    private final SpliffySecurityManager securityManager;
    private final ApplicationManager applicationManager;
    private final EventManager eventManager;
    private final SessionManager sessionManager;
    private final CurrentRootFolderService currentRootFolderService;
    private List<File> roots; // list of directories in which to look for static resources
    private final Date loadedDate = new Date();

    public SpliffyResourceFactory(ServletContext servletContext, UserDao userDao, SpliffySecurityManager securityManager, ApplicationManager applicationManager, EventManager eventManager, SessionManager sessionManager, CurrentRootFolderService currentRootFolderService) {
        this.userDao = userDao;
        this.securityManager = securityManager;
        this.applicationManager = applicationManager;
        this.eventManager = eventManager;
        this.sessionManager = sessionManager;
        this.currentRootFolderService = currentRootFolderService;

        roots = new ArrayList<>();
        File fWebappRoot = new File(servletContext.getRealPath("/"));
        roots.add(fWebappRoot);
        log.info("Using webapp root dir: " + fWebappRoot.getAbsolutePath());

        String extraRoots = System.getProperty(ROOTS_SYS_PROP_NAME);
        if (extraRoots != null && !extraRoots.isEmpty()) {
            String[] arr = extraRoots.split(",");
            for (String s : arr) {
                File root = new File(s);
                if (!root.exists()) {
                    throw new RuntimeException("Root template dir specified in system property does not exist: " + root.getAbsolutePath() + " from property value: " + extraRoots);
                }
                roots.add(root);
                log.info("Using file template root: " + root.getAbsolutePath());
            }
        }
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        if (log.isTraceEnabled()) {
            log.trace("getResource: " + sPath);
        }
        Path path = Path.path(sPath);
        Resource r;
        if (path.getName() != null && path.getName().equals("_triplets")) { // special path suffix to get sync triplets for a directory. See HttpTripletStore
            r = find(host, path.getParent());
            if (r != null) {
                if (r instanceof ContentDirectoryResource) {
                    ContentDirectoryResource dr = (ContentDirectoryResource) r;
                    r = new DirectoryHashResource(dr.getDirectoryNode().getHash(), securityManager, dr);
                }
            }
        } else {
            r = findResource(host, path);
        }
        if (r != null) {
            log.debug("Found a resource: " + r.getClass());
        } else {
            log.debug("Not found: " + sPath);
        }
        return r;
    }

    public Resource findResource(String host, Path path) throws NotAuthorizedException, BadRequestException {
        if (path.getFirst() != null && path.getFirst().equals("templates")) {
            RootFolder rootFolder = currentRootFolderService.getRootFolder(host);
            Resource override = findStaticTemplateResourceOverride(rootFolder, path);
            if (override != null) {
                return override;
            }
        }
        Resource r = find(host, path);
        if (r == null) {
            if (path.getName().endsWith(".new")) {
                // Not found, but a html page is requested. If the parent exists and is a collection
                // then we'll instantiate a placeholder page which will allow new pages to be created
                Resource rParent = find(host, path.getParent());
                if (rParent instanceof ContentDirectoryResource) {
                    ContentDirectoryResource parentContentDir = (ContentDirectoryResource) rParent;
                    return new NewPageResource(parentContentDir, path.getName());
                }
            } else {
                // if the requested url is in a theme folder, then try to resolve to a static theme resource
                RootFolder rootFolder = currentRootFolderService.getRootFolder(host);
                Resource themeResource = findStaticResource(rootFolder, path);
                r = themeResource;
            }
        }
        return r;
    }

    private Resource find(String host, Path p) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        if (p.isRoot()) {
            Resource rootFolder = currentRootFolderService.getRootFolder(host);
            return rootFolder;
        } else {
            Path pPathParent = p.getParent();
            if (pPathParent == null) {
                return null;
            }
            Resource rParent = find(host, p.getParent());
            if (rParent == null) {
                return null;
            } else {
                if (rParent instanceof RootFolder && p.getName().equals(".dologin")) {
                    RootFolder rf = (RootFolder) rParent;
                    return new SpliffyAjaxLoginResource(rf, p.getName());
                }

                if (rParent instanceof CollectionResource) {
                    CollectionResource parent = (CollectionResource) rParent;
                    Resource r = parent.child(p.getName());
                    return r;
                } else {
                    return null;
                }
            }
        }
    }

    public Resource findFromRoot(RootFolder rootFolder, Path p) throws NotAuthorizedException, BadRequestException {
        CollectionResource col = rootFolder;
        Resource r = null;
        for (String s : p.getParts()) {
            if (col == null) {
                return null;
            }
            r = col.child(s);
            if (r == null) {
                return null;
            }
            if (r instanceof CollectionResource) {
                col = (CollectionResource) r;
            } else {
                col = null;
            }
        }
        return r;
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

    /**
     * This is called if no content item could be found under /theme
     *
     * We should look first for a resource in the theme folder for the
     *
     * @param host
     * @param sPath
     * @param path
     * @return
     */
    private Resource findStaticResource(RootFolder rf, Path path) {
        switch (path.getFirst()) {
            case "theme":
                return findStaticThemeResource(rf, path);
            case "templates":
                return findStaticTemplateResource(rf, path);
        }
        return null;
    }

    private Resource findStaticThemeResource(RootFolder rf, Path path) {
        if (log.isTraceEnabled()) {
            log.trace("findStaticThemeResource: " + path);
        }
        Path relPath = path.getStripFirst();
        String internalTheme = "admin";
        if (rf instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rf;
            internalTheme = wrf.getBranch().getPublicTheme();
        }
        //return find(rf, internalTheme, internalTheme);
        // Check if exists as a file resource (ie unpacked servlet resource) in the theme
        String staticThemeResPath = "/templates/themes/" + internalTheme + relPath;
        if (log.isTraceEnabled()) {
            log.trace("Check roots: " + roots.size());
        }
        for (File root : roots) {
            File file = new File(root, staticThemeResPath);
            if (!file.exists()) {
                if (log.isTraceEnabled()) {
                    log.trace("file not found in root: " + root.getAbsolutePath());
                }
                //log.info("resource does not exist: " + templateFile.getAbsolutePath());
            } else {
                if (file.isFile()) {
                    log.trace("found file: " + file.getAbsolutePath());
                    //String contentType = ContentTypeUtils.findContentTypes(file.getName());
                    return new StaticResource(file);
                }
            }
        }

        // Check if exists as a file resource in apps
        if (relPath.getFirst().equals("apps")) {
            String appPath = "templates" + relPath;
            if (log.isTraceEnabled()) {
                log.trace("Check if exists in apps dir: " + appPath + " in roots: " + roots.size());
            }
            for (File root : roots) {
                if( log.isTraceEnabled() ) {
                    log.trace("Check for app resource in root: " + root.getAbsolutePath());
                }
                File file = new File(root, appPath);
                if (!file.exists()) {
                    if( log.isTraceEnabled()) {
                        log.trace("app resource does not exist: " + file.getAbsolutePath());
                    }
                } else {
                    if (file.isFile()) {
                        log.trace("found file: " + file.getAbsolutePath());
                        //String contentType = ContentTypeUtils.findContentTypes(file.getName());
                        return new StaticResource(file);
                    }
                }
            }

        }

        // ok, last chance, look for a classpath resource
        String cpPath = "/templates" + relPath;
        if (log.isTraceEnabled()) {
            log.trace("Check in classpath: " + cpPath);
        }
        URL resource = this.getClass().getResource(cpPath);
        if (resource != null) {
            String contentType = ContentTypeUtils.findContentTypes(path.getName());
            return new UrlResource(path.getName(), resource, contentType, loadedDate);
        }
        log.trace("Not found in website repository themes, or in apps, or as a classpath resource");
        return null;
    }

    /**
     * Check if there is a theme resource which overrides a template resource
     *
     * @param rf
     * @param path
     * @return
     */
    public Resource findStaticTemplateResourceOverride(RootFolder rf, Path path) {
        Path relPath = path.getStripFirst();
        String internalTheme = "admin";
        if (rf instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rf;
            internalTheme = wrf.getBranch().getPublicTheme();
        }
        //return find(rf, internalTheme, internalTheme);
        // Check if exists as a file resource (ie unpacked servlet resource) in the theme
        String staticThemeResPath = "/templates/themes/" + internalTheme + relPath;
        Path p = Path.path(staticThemeResPath);
        Resource r = findStaticTemplateResource(rf, p);
        return r;
    }

    public Resource findStaticTemplateResource(RootFolder rf, Path path) {
        for (File root : roots) {
            File file = new File(root, path.toString());
            if (!file.exists()) {
                //log.info("resource does not exist: " + templateFile.getAbsolutePath());
            } else {
                if (file.isFile()) {
                    log.trace("found file: " + file.getAbsolutePath());
                    //String contentType = ContentTypeUtils.findContentTypes(file.getName());
                    return new StaticResource(file);
                }
            }
        }

        // ok, last chance, look for a classpath resource
        URL resource = this.getClass().getResource(path.toString());
        if (resource != null) {
            String contentType = ContentTypeUtils.findContentTypes(path.getName());
            return new UrlResource(path.getName(), resource, contentType, loadedDate);
        }
        return null;
    }
}
