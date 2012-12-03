/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.fileserver;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.resource.Resource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class FileServerApp implements LifecycleApplication, MenuApplication, ChildPageApplication {

    public static final int DEFAULT_PUSH_SYNC_PORT = 7020;
    public static final String DEFAULT_PUSH_SYNC_NAME = "push.sync.port";
    private PushManager pushManager;

    @Override
    public String getInstanceId() {
        return "fileSyncPush";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        Integer port = config.getInt(DEFAULT_PUSH_SYNC_NAME);
        if (port == null) {
            port = DEFAULT_PUSH_SYNC_PORT;
        }
        CurrentRootFolderService currentRootFolderService = config.getContext().get(CurrentRootFolderService.class);
        SessionManager sessionManager = resourceFactory.getSessionManager();        
        pushManager = new PushManager(port, resourceFactory.getEventManager(), resourceFactory.getSecurityManager(), currentRootFolderService, sessionManager, config.getContext()); 
        config.getContext().put(pushManager);

        pushManager.start();
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "File sync push";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Notifies connected file sync clients when a file change has occured on the server";
    }

    @Override
    public void shutDown() {
        pushManager.stop();
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
        config.setInt(DEFAULT_PUSH_SYNC_NAME, DEFAULT_PUSH_SYNC_PORT);
    }

    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
        if (parentOrg == null) {
            return;
        }
        Path parentPath = parentOrg.getPath();
        switch (parentId) {
            case "menuDashboard":
                //parent.getOrCreate("menuFileManager", "File manager").setOrdering(40);
                break;
            case "menuFileManager":
                parent.getOrCreate("menuManageRepos", "Manage repositories", parentPath.child("manageRepos")).setOrdering(10);
                break;
        }
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationFolder) {
            CommonCollectionResource p = (CommonCollectionResource) parent;
            switch (requestedName) {
                case "manageRepos":
                    MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos");
                    return new ManageRepositoriesFolder(requestedName, p.getOrganisation(), p);

            }
        }
        return null;
    }
}
