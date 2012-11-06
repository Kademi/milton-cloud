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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.ManageWebsiteBranchFolder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.GetableResource;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.vfs.db.Group;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageDashboardMessagesListPage extends AbstractResource implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageDashboardMessagesListPage.class);
    private final OrganisationFolder parent;
    private final String name;

    public ManageDashboardMessagesListPage(OrganisationFolder parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public String getTitle() {
        return "Manage dashboard messages";
    }
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuTalk", "menuDashboardMessages", "menuDashboardMessages");
        _(HtmlTemplater.class).writePage("admin", "dashboard/manageDashboardMessagesList", this, params, out);
    }

    /**
     * Returns a ManageDashboardMessagePage, representing the given group in website branch
     * 
     * Use this to generate a href for the page
     * 
     * @return
     * @throws NotAuthorizedException
     * @throws BadRequestException 
     */
    public ManageDashboardMessagePage getManageDashboardPage(ManageWebsiteBranchFolder bf, Group group) throws NotAuthorizedException, BadRequestException {
        ManageDashboardMessagePage p = new ManageDashboardMessagePage(group, bf);
        return p;
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
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
