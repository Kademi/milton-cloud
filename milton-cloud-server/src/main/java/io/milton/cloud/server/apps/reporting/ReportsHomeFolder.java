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
package io.milton.cloud.server.apps.reporting;

import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.ReportingApplication;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.vfs.db.Website;

/**
 *
 * @author brad
 */
public class ReportsHomeFolder extends AbstractCollectionResource implements GetableResource {

    public static String HOME_NAME = "reporting";
    
    private final CommonCollectionResource parent;
    private final String name;
    private final ApplicationManager applicationManager;
    private ResourceList children;

    public ReportsHomeFolder( CommonCollectionResource parent) {
        this.name = ReportsHomeFolder.HOME_NAME;
        this.parent = parent;
        applicationManager = _(ApplicationManager.class);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuReporting");
        _(HtmlTemplater.class).writePage("reporting/home", this, params, out);
    }



    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            
            // Add per website reports
            if (getOrganisation().getWebsites() != null) {
                for (Website w : getOrganisation().getWebsites()) {
                    WebsiteReportsFolder f = new WebsiteReportsFolder(w, parent);
                    children.add(f);
                }
            }
            
            // Add reports for organisation level
            RootFolder rootFolder = WebUtils.findRootFolder(this);
            for( Application app : applicationManager.getActiveApps(rootFolder) ) {
                if( app instanceof ReportingApplication ) {
                    ReportingApplication rapp = (ReportingApplication) app;
                    for( JsonReport rep : rapp.getReports(getOrganisation(), null)) {
                        String title = rep.getTitle(getOrganisation(), null);
                        children.add(new ReportPage("org-" + rep.getReportId(), this, title + " Report", rep, null));
                        children.add(new ReportCsvPage("org-" + rep.getReportId() + ".csv", this, title + " CSV", rep, null));                        
                        children.add(new ReportChartPage("org-" + rep.getReportId() + ".png", this, title + " Chart", rep, null));
                    }
                }
            }
            
            applicationManager.addBrowseablePages(this, children);
        }
        return children;
    }

    public String getTitle() {
        return "Reports home: " + getOrganisation().getTitle();
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
    }
}
