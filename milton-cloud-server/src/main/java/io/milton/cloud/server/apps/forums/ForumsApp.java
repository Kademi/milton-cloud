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
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.Forum;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TextTemplater;
import io.milton.common.Path;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.Context;

import static io.milton.context.RequestContext._;
import io.milton.http.AclUtils;
import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.Date;
import java.util.Set;

/**
 *
 * @author brad
 */
public class ForumsApp implements MenuApplication, ResourceApplication, PortletApplication, ChildPageApplication, BrowsableApplication {

    public static final String ROLE_FORUM_VIEWER = "Forums viewer";
    public static final String ROLE_FORUM_USER = "Forums user";
    
    public static String toHref(ForumPost r) {
        Forum forum = r.getForum();
        Website website = forum.getWebsite();
        String sPort = _(Formatter.class).getPortString();
        String path = "/community/" + forum.getName() + "/" + r.getName();
        String url = "http://" + website.getDomainName() + sPort + path;
        return url;
    }

    private Date modDate = new Date();
    
    @Override
    public String getInstanceId() {
        return "forums";
    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        return "Forums, topics and questions";
    }

    
    
    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Provides forums for end users to ask questions and answer them, and gives administrators functions to manage them";
    }
    
    

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        resourceFactory.getSecurityManager().add(new ForumViewerRole() );
        resourceFactory.getSecurityManager().add(new ForumUserRole());
        
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof WebsiteRootFolder) {
            if (requestedName.equals("_postSearch")) {                
                WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
                return new PostSearchResource(requestedName, wrf.getWebsite(), wrf);
            }
        } else if( parent instanceof OrganisationFolder) {
            OrganisationFolder repoFolder = (OrganisationFolder) parent;
            if( requestedName.equals("managePosts")) {
                return new ManagePostsPage(requestedName, repoFolder.getOrganisation(), repoFolder);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder repoFolder = (OrganisationFolder) parent;
            ManageForumsFolder forumsAdminFolder = new ManageForumsFolder("forums", repoFolder);
            children.add(forumsAdminFolder);
        } else if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            children.add(new MyForumsFolder("community", wrf, wrf.getWebsite()));
        }
    }

    @Override
    public void appendMenu(MenuItem parent) {
        switch (parent.getId()) {
            case "menuRoot":
                if (parent.getRootFolder() instanceof WebsiteRootFolder) {
                    parent.getOrCreate("menuCommunity", "Community", "/community").setOrdering(40);
                } else {
                    parent.getOrCreate("menuTalk", "Talk &amp; Connect").setOrdering(30);
                }
                break;
            case "menuTalk":
                parent.getOrCreate("menuManageForums", "Manage forums").setOrdering(10);
                break;
            case "menuManageForums":
                OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
                if (parentOrg != null) {
                    parent.getOrCreate("menuManagePosts", "Manage posts", parentOrg.getPath().child("managePosts")).setOrdering(10);
                    parent.getOrCreate("menuEditForums", "Create and manage forums", parentOrg.getPath().child("forums")).setOrdering(20);
                }
                break;
        }

    }

    @Override
    public Resource getResource(RootFolder webRoot, String path) {
        if (!path.startsWith("/templates/apps/forum")) {
            return null;
        }
        if (path.endsWith(".css")) {
            Path p = Path.path(path);
            TemplatedTextPage t = new TemplatedTextPage(p.getName(), webRoot, "text/css", path);
            t.setModifiedDate(modDate);
            return t;
        }
        return null;
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (portletSection.equals("dashboardSecondary")) {
            _(TextTemplater.class).writePage("forums/recentPostsPortlet.html", currentUser, rootFolder, context, writer);
        }
    }
    
    public class ForumUserRole implements Role {

        @Override
        public String getName() {
            return ROLE_FORUM_USER;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if( resource instanceof IForumResource ) {
                IForumResource acr = (IForumResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }            
            return false;
        }

        @Override
        public Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {            
            return AclUtils.asSet(AccessControlledResource.Priviledge.READ, AccessControlledResource.Priviledge.WRITE_CONTENT);
        }
    }    
    
    public class ForumViewerRole implements Role {

        @Override
        public String getName() {
            return ROLE_FORUM_VIEWER;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if( resource instanceof IForumResource ) {
                IForumResource acr = (IForumResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }            
            return false;
        }

        @Override
        public Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {            
            return AclUtils.asSet(AccessControlledResource.Priviledge.READ);
        }
    }       
}
