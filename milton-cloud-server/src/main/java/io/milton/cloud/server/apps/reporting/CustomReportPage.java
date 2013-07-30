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
import io.milton.cloud.server.db.CustomReport;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Arrays;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class CustomReportPage extends AbstractResource implements GetableResource, TitledPage, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(CustomReportPage.class);
    public static String HOME_NAME = "reporting";
    private final CommonCollectionResource parent;
    private final CustomReport customReport;
    private final ApplicationManager applicationManager;
    private Map<String, String> mapOfDataSources;
    protected JsonResult jsonResult;

    public CustomReportPage(CommonCollectionResource parent, CustomReport customReport) {
        this.parent = parent;
        this.customReport = customReport;
        applicationManager = _(ApplicationManager.class);
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        // Editign details
        try {
            _(DataBinder.class).populate(customReport, parameters);
            session.save(customReport);
            tx.commit();
            jsonResult = new JsonResult(true);
        } catch (Throwable ex) {
            tx.rollback();
            jsonResult = new JsonResult(false);
            log.error("exception updating", ex);
            jsonResult.setMessages(Arrays.asList("Failed to update: " + ex.getMessage()));
        }


        return null;
    }

    public CustomReport getReport() {
        return customReport;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuReporting");
        _(HtmlTemplater.class).writePage("reporting/manageCustomReportPage", this, params, out);
    }

    @Override
    public String getTitle() {
        return "Custom Report: " + customReport.getTitle();
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
        return customReport.getName();
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

    public Map<String, String> getDatasources() {
        if (mapOfDataSources == null) {
            for (Application app : applicationManager.findActiveApps(getOrganisation())) {
                if (app instanceof ReportingApplication) {
                    ReportingApplication rapp = (ReportingApplication) app;
                    List<ReportingApplication.CustomReportDataSource> srcs = rapp.getDataSources();
                    if (srcs != null) {
                        for (ReportingApplication.CustomReportDataSource src : srcs) {
                            mapOfDataSources.put(src.getId(), src.getTitle());
                        }
                    }
                }
            }
        }
        return mapOfDataSources;
    }
}
