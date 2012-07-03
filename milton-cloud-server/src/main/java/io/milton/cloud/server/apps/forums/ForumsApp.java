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

import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplateRenderer;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;

/**
 *
 * @author brad
 */
public class ForumsApp implements MenuApplication, ResourceApplication {

    public static String toHref(ForumPost r) {
        return "todo";
    }

    @Override
    public String getInstanceId() {
        return "forums";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof ForumsAdminFolder) {
            ForumsAdminFolder faf = (ForumsAdminFolder) parent;
            if (requestedName.equals("manage")) {
                MenuItem.setActiveIds("menuTalk", "menuManageForums", "menuEditForums");
                return new ManageForumsPage(requestedName, faf.getOrganisation(), faf);
            }
        } else if (parent instanceof OrganisationFolder) {
            OrganisationFolder orgFolder = (OrganisationFolder) parent;
            if (requestedName.equals("manageForums")) {
                MenuItem.setActiveIds("menuTalk", "menuPrograms", "menuEditForums");
                return new ManageForumsPage(requestedName, orgFolder.getOrganisation(), orgFolder);
            } else if (requestedName.equals("managePosts")) {
                MenuItem.setActiveIds("menuTalk", "menuPrograms", "menuManagePosts");
                return new ManagePostsPage(requestedName, orgFolder.getOrganisation(), orgFolder);
            }
        } else if (parent instanceof WebsiteRootFolder) {
            if (requestedName.equals("_postSearch")) {
                WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
                return new PostSearchResource(requestedName, wrf.getWebsite(), wrf);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof RepositoryFolder) {
            RepositoryFolder repoFolder = (RepositoryFolder) parent;
            Repository r = repoFolder.getRepository();
            List<Website> list = Website.findByWebsite(r, SessionManager.session());
            if (!list.isEmpty()) {
                Website w = list.get(0); // should only ever be 1
                ForumsAdminFolder forumsAdminFolder = new ForumsAdminFolder("forums", repoFolder, w);
                children.add(forumsAdminFolder);
            }
        } else if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            children.add(new MyForumsFolder("forums", wrf, wrf.getWebsite()));
        }
    }

    @Override
    public void appendMenu(MenuItem parent) {
        OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
        if (parentOrg != null) {
            switch (parent.getId()) {
                case "menuRoot":
                    parent.getOrCreate("menuTalk", "Talk &amp; Connect").setOrdering(30);
                    break;
                case "menuTalk":
                    parent.getOrCreate("menuManageForums", "Manage forums").setOrdering(10);
                    break;
                case "menuManageForums":
                    parent.getOrCreate("menuManagePosts", "Manage posts", parentOrg.getPath().child("managePosts")).setOrdering(10);
                    parent.getOrCreate("menuEditForums", "Create and manage forums", parentOrg.getPath().child("manageForums")).setOrdering(20);
                    break;
            }
        }
    }

    @Override
    public Resource getResource(WebsiteRootFolder webRoot, String path) {
        if (!path.startsWith("/templates/apps/forum")) {
            return null;
        }
        if (path.endsWith(".css")) {
            Path p = Path.path(path);
            return new TemplatedTextPage(p.getName(), webRoot, "text/css", path);
        }
        return null;
    }
}
