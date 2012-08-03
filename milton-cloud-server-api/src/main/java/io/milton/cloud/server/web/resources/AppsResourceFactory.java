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
package io.milton.cloud.server.web.resources;

import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.RootFolder;
import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;

/**
 * Locates resources provided by applications
 *
 * This can be easier and faster then building a tree structure, especially for
 * static insecure resources such as css files and images
 *
 * @author brad
 */
public class AppsResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AppsResourceFactory.class);
    private final ApplicationManager applicationManager;

    public AppsResourceFactory(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }
        RootFolder rootFolder = (RootFolder) applicationManager.getPage(null, host);

        for (Application app : applicationManager.getApps()) {
            if (app instanceof ResourceApplication) {
                ResourceApplication ra = (ResourceApplication) app;
                Resource r = ra.getResource(rootFolder, sPath);
                if (r != null) {
                    return r;
                }
            }
        }

        return null;
    }
}
