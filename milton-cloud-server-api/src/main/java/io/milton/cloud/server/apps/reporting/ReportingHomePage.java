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
package io.milton.cloud.server.apps.reporting;

import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;

/**
 * Dashboard for the user. Most functions will be provided by PortletApplications
 * which will be invoked from the template using the PortletsDirective
 * 
 * Eg:
 * #portlets("messages") 
 * , which will render portlets for the messages section of the page
 * 
 * Standard sections are intended to be:
 * messages - brief list of messages at top of page
 * primary - this is the main section of the page, with about 70% width
 * secondary - this is a narrowed section of the page, possible lower down for small screen clients
 * 
 *
 * @author brad
 */
public class ReportingHomePage extends AbstractResource implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(ReportingHomePage.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private final Website website; // optional
    private JsonResult jsonResult;

    public ReportingHomePage(String name, Organisation organisation, CommonCollectionResource parent, Website website) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
        this.website = website;        
    }
    
    public String getTitle() {
        return "Reporting";
    }


    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "admin/manageApps", this, params, out);
        }
    }


    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
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
    public Organisation getOrganisation() {
        return organisation;
    }

    @Override
    public boolean is(String type) {
        if (type.equals("apps")) {
            return true;
        }
        return super.is(type);
    }
}
