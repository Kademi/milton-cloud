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

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.CustomReport;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NewPageResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.WebUtils;
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
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class CustomReportsHomeFolder extends AbstractCollectionResource implements GetableResource, TitledPage, PostableResource {

    public static String HOME_NAME = "reporting";
    private final CommonCollectionResource parent;
    private final String name;
    private final ApplicationManager applicationManager;
    private ResourceList children;
    private JsonResult jsonResult;

    public CustomReportsHomeFolder(CommonCollectionResource parent, String name) {
        this.name = name;
        this.parent = parent;
        applicationManager = _(ApplicationManager.class);
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        String newTitle = WebUtils.getRawParam(parameters, "newTitle");
        if (newTitle != null) {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            String newName = NewPageResource.findAutoCollectionName(newTitle, this, parameters);
            Profile creator = _(SpliffySecurityManager.class).getCurrentUser();
            CustomReport customReport = CustomReport.create(getOrganisation(), newName, newTitle);
            session.save(customReport);
            tx.commit();
            jsonResult = new JsonResult(true, "Created", customReport.getName());
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuReporting");
            _(HtmlTemplater.class).writePage("reporting/manageCustomReportsHome", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for (CustomReport r : CustomReport.findByOrg(getOrganisation(), SessionManager.session())) {
                CustomReportPage p = new CustomReportPage(this, r);
                children.add(p);
            }
            applicationManager.addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public String getTitle() {
        return "Custom Reports home: " + getOrganisation().getTitle();
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
