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

import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageOrgTypesFolder extends AbstractResource implements GetableResource, CommonCollectionResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageOrgTypesFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private JsonResult jsonResult;
    private ResourceList children;

    public ManageOrgTypesFolder(String name, Organisation organisation, CommonCollectionResource parent) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            String name =WebUtils.getParam(parameters, "name");
            OrgType orgType = organisation.createOrgType(name, session);
            String displayName = WebUtils.getParam(parameters, "displayName");
            orgType.setDisplayName(displayName);
            session.save(orgType);
            jsonResult = new JsonResult(true, "Created");
            ManageOrgTypePage newPage = new ManageOrgTypePage(this, orgType);
            jsonResult.setNextHref(newPage.getHref());
            tx.commit();
        } catch(Exception e) {
            tx.rollback();
            log.error("ex", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }    
    
    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ResourceList();
            for( OrgType g : organisation.getOrgTypes() ) {
                ManageOrgTypePage mgf = new ManageOrgTypePage(this,g);
                children.add(mgf);
            }
        }
        return children;
    }    
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }    

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuOrgTypes");
            _(HtmlTemplater.class).writePage("admin", "admin/manageOrgTypes", this, params, out);
        }
    }

    public String getTitle() {
        return "Manage Organisation Types";
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
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return organisation;
    }
}
