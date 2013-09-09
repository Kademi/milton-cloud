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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.server.apps.website.AppsPageHelper;
import io.milton.cloud.server.apps.website.ManageWebsiteBranchFolder;
import io.milton.cloud.server.apps.website.ManageWebsitesFolder;
import io.milton.cloud.server.apps.website.ManageWebsiteFolder;
import edu.emory.mathcs.backport.java.util.Collections;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.DataResourceApplication;
import io.milton.cloud.server.apps.FolderViewApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ReportingApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.reporting.ReportingApp;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.text.TextFromHtmlService;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import static io.milton.context.RequestContext._;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.Set;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author brad
 */
public class AdminApp implements MenuApplication, ReportingApplication, ChildPageApplication, PortletApplication, DataResourceApplication, BrowsableApplication, FolderViewApplication {

    public static final String ALT_TXT_SUFFIX = ".alt.txt";
    
    private ApplicationManager applicationManager;
    private List<JsonReport> reports;
    private AppsPageHelper appsPageHelper;

    public AdminApp() {
        reports = new ArrayList<>();
        reports.add(new WebsiteAccessReport());
    }

    @Override
    public String getInstanceId() {
        return "admin";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Administration";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        applicationManager = _(ApplicationManager.class);
        appsPageHelper = new AppsPageHelper(applicationManager);
        config.getContext().put(appsPageHelper);
        resourceFactory.getSecurityManager().add(new AdminRole());
        resourceFactory.getSecurityManager().add(new AdminViewerRole());
        resourceFactory.getSecurityManager().add(new UserAdminRole());
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides most admin console functionality, such as managing users, groups, websites, etc";
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationRootFolder) {
            if (requestedName.equals("maint")) {
                OrganisationRootFolder rf = (OrganisationRootFolder) parent;
                if( rf.getOrganisation().getOrganisation() == null ) {
                    return new MaintPage(rf);
                }
            }
        }
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder p = (OrganisationFolder) parent;
            switch (requestedName) {
                case "manageUsers":
                    MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuUsers");
                    return new ManageUsersFolder(requestedName, p.getOrganisation(), p);
                case "manageApps":
                    MenuItem.setActiveIds("menuDashboard", "menuWebsiteManager", "manageApps");
                    return new ManageAppsPage(requestedName, p.getOrganisation(), p);
                case "manageDashboardMessages":
                    return new ManageDashboardMessagesListPage(p, requestedName);

            }
        } else if (parent instanceof ManageWebsiteBranchFolder) {
            ManageWebsiteBranchFolder bf = (ManageWebsiteBranchFolder) parent;
            if (requestedName.equals("publish")) {
                MenuItem.setActiveIds("menuDashboard", "menuWebsiteManager");
                return new PublishBranchPage(requestedName, bf);
            } else if (requestedName.startsWith("_dashboard_")) {
                String groupName = requestedName.replace("_dashboard_", "");
                Group g = bf.getOrganisation().group(groupName, SessionManager.session());
                if (g != null) {
                    return new ManageDashboardMessagePage(g, bf);
                }
            }
        }
        return null;
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
            case "menuRoot":
                parent.getOrCreate("menuDashboard", "My Dashboard", parentPath).setOrdering(10);
                break;
            case "menuDashboard":
                parent.getOrCreate("menuGroupsUsers", "Groups &amp; users").setOrdering(20);
                parent.getOrCreate("menuWebsiteManager", "Website manager").setOrdering(30);
                break;
            case "menuGroupsUsers":
                parent.getOrCreate("menuUsers", "Manage users", parentPath.child("manageUsers")).setOrdering(10);
                parent.getOrCreate("menuGroups", "Manage groups", parentPath.child("groups")).setOrdering(20);
                Path p = parentOrg.getPath().child("organisations");
                parent.getOrCreate("menuOrgs", "Manage Business units", p + "/").setOrdering(30);
                if (parentOrg instanceof OrganisationRootFolder) { // only show this for root admin folder
                    parent.getOrCreate("menuOrgTypes", "Manage Organisation Types", parentOrg.getPath().child("orgTypes") + "/").setOrdering(30);
                }
                break;
            case "menuWebsiteManager":
                parent.getOrCreate("menuWebsites", "Manage websites", parentPath.child("websites")).setOrdering(10);
                //parent.getOrCreate("menuThemes", "Templates &amp; themes", parentPath.child("themes")).setOrdering(20);
                parent.getOrCreate("menuApps", "Applications", parentPath.child("manageApps")).setOrdering(30);
                break;
            case "menuTalk":
                parent.getOrCreate("menuDashboardMessages", "Dashboard messages").setOrdering(10);
                break;
            case "menuDashboardMessages":
                parent.getOrCreate("menuGroupDashboardMessages", "Group dashboard messages", parentOrg.getPath().child("manageDashboardMessages")).setOrdering(60);
                break;

        }
    }

    @Override
    public List<JsonReport> getReports(Organisation org, Website website) {
        return reports;
    }

    /**
     * This is used to display the web access report on the admin dashboard
     *
     * @param portletSection
     * @param currentUser
     * @param rootFolder
     * @param context
     * @param writer
     * @throws IOException
     */
    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (rootFolder instanceof OrganisationRootFolder && currentUser != null) {
            if (portletSection.equals("adminDashboardPrimary")) {
                renderDashboardPortlets(writer, context);
            }
        }

    }

    @Override
    public ContentResource instantiateResource(Object o, CommonCollectionResource parent, RootFolder rf) {
        if (parent instanceof ManageWebsiteFolder && o instanceof Branch) {
            Branch b = (Branch) o;
            Repository r = b.getRepository();
            Website w = (Website) r;
            return new ManageWebsiteBranchFolder(w, b, parent);
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof OrganisationFolder) {
            CommonCollectionResource p = (CommonCollectionResource) parent;
            children.add(new ManageWebsitesFolder("websites", p.getOrganisation(), p));            
            children.add( new ManageGroupsFolder("groups", p.getOrganisation(), p) );
            
        }
        // Only show orgTypes page for root admin folder
        if (parent instanceof OrganisationRootFolder) {
            CommonCollectionResource p = (CommonCollectionResource) parent;
            children.add(new ManageOrgTypesFolder("orgTypes", p.getOrganisation(), p));
        }
    }

    private void renderDashboardPortlets(Writer writer, Context context) throws IOException {
        CommonResource r = (CommonResource) context.get("page");
        OrganisationFolder orgFolder = WebUtils.findParentOrg(r);
        if (orgFolder != null) {
            Organisation org = orgFolder.getOrganisation();
            if (Utils.isEmpty(org.getWebsites())) {
                renderDashboardCreateWebsitePortlet(writer, orgFolder);
            } else {
                renderDashboardReports(writer, orgFolder);
            }
        }
    }

    private void renderDashboardReports(Writer writer, OrganisationFolder orgFolder) throws IOException {
        writer.append("<div class='report'>\n");
        writer.append("<h3>Website activity</h3>\n");
        writer.append("<div class='websiteAccess'></div>\n");
        writer.append("<script type='text/javascript' >\n");
        writer.append("jQuery(function() {\n");
        //17/09/2012 - 24/09/2012
        String range = ReportingApp.getDashboardDateRange();
        if (orgFolder != null) {
            //http://localhost:8080/organisations/3dn/reporting/org-learningProgress?startDate=Choose+a+date+range&finishDate=
            String href = orgFolder.getHref() + "reporting/org-websiteAccess";
            writer.append(" runReport(\"" + range + "\", jQuery('.report .websiteAccess'), null, \"" + href + "\");\n");
            writer.append("});\n");
            writer.append("</script>\n");
        }
        writer.append("</div>\n");
    }

    private void renderDashboardCreateWebsitePortlet(Writer writer, OrganisationFolder orgFolder) throws IOException {
        writer.append("<div class='wizard'>\n");
        writer.append("<h3>Getting started</h3>\n");
        writer.append("<p>To get started you should probably create a website <button class='createWebsite Btn'>Create a website</button></p>\n");

        writer.append("</div>\n");
    }

    @Override
    public List<CustomReportDataSource> getDataSources() {
        return null;
    }

    /**
     * Renders a directory
     * 
     * @param folder
     * @param out
     * @param params
     * @param contentType
     * @throws IOException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     * @throws NotFoundException 
     */
    @Override
    public void renderPage(ContentDirectoryResource folder, OutputStream out, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos"); // For admin
        _(HtmlTemplater.class).writePage("admin/manageFiles", folder, params, out);
    }

    @Override
    public boolean supports(RootFolder rf) {
        return rf instanceof OrganisationRootFolder;
    }

    public class AdminRole implements Role {

        @Override
        public String getName() {
            return "Administrator";
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            Organisation resourceOrg = resource.getOrganisation();
            boolean b = resourceOrg.isWithin(withinOrg);
            return b;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Collections.singleton(Priviledge.ALL);
        }

        @Override
        public boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g) {
            if (resource instanceof CommonRepositoryResource) {
                CommonRepositoryResource cr = (CommonRepositoryResource) resource;
                return (cr.getRepository() == applicableRepo);
            } else {
                return false;
            }
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g) {
            return Collections.singleton(Priviledge.ALL);
        }
    }
    
    public class AdminViewerRole implements Role {

        @Override
        public String getName() {
            return "AdminViewer";
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            Organisation resourceOrg = resource.getOrganisation();
            boolean b = resourceOrg.isWithin(withinOrg);
            return b;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Role.READ;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g) {
            if (resource instanceof CommonRepositoryResource) {
                CommonRepositoryResource cr = (CommonRepositoryResource) resource;
                return (cr.getRepository() == applicableRepo);
            } else {
                return false;
            }
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g) {
            return Role.READ;
        }
    }    

    public class UserAdminRole implements Role {

        @Override
        public String getName() {
            return "User Administrator";
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if (resource instanceof UserResource) {
                UserResource ur = (UserResource) resource;
                return ur.getOrganisation().isWithin(withinOrg);
            }
            return false;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Role.READ_WRITE;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g) {
            return false;
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g) {
            return null;
        }
    }
}
