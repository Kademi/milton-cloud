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
package io.milton.cloud.server.apps.orgs;

import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.cloud.server.db.utils.WebsiteDao;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class OrganisationsApp implements Application {

    private final WebsiteDao websiteDao = new WebsiteDao();
    private Services services;
    private ApplicationManager applicationManager;

    @Override
    public String getInstanceId() {
        return "website";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        services = resourceFactory.getServices();
        applicationManager = services.getApplicationManager();
    }

    /**
     * For a root resource (ie where parent is null) the requestedname will be
     * the hostname
     *
     * @param parent
     * @param requestedName
     * @return
     */
    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent == null) {
            Website website = websiteDao.getWebsite(requestedName, SessionManager.session());
            if (website == null) {
                Organisation org = OrganisationDao.getRootOrg(SessionManager.session());
                if (org == null) {
                    throw new RuntimeException("No root organisation");
                }
                return new OrganisationFolder(services, applicationManager, org);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, List<Resource> children) {
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
