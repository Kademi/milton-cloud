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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageOrgsPage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageOrgsPage.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private JsonResult jsonResult;

    public ManageOrgsPage(String name, Organisation organisation, CommonCollectionResource parent) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {        
        String newTitle = parameters.get("newTitle");
        if (newTitle != null) {
            log.info("processForm: newTitle: " + newTitle);
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            String newName = NewPageResource.findAutoName(newTitle, this.getParent(), parameters);
            Organisation c = getOrganisation().createChildOrg(newName, session);
            session.save(c);
            
            Date now = _(CurrentDateService.class).getNow();
            Profile curUser= _(SpliffySecurityManager.class).getCurrentUser();            
            AppControl.initDefaultApps(organisation, curUser, now, session);
            
            tx.commit();
            jsonResult = new JsonResult(true, "Created", c.getName());
        }
        return null;
    }
    
    public String getTitle() {
        return "Manage business units";
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "admin/manageOrgs", this, params, out);
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

    public List<Organisation> getChildOrganisations() {
        List<Organisation> list = new ArrayList<>();
        List<BaseEntity> members = getOrganisation().getMembers();
        if (members == null || members.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        for (BaseEntity be : members) {
            if (be instanceof Organisation) {
                list.add((Organisation) be);
            }
        }
        return list;
    }

    @Override
    public boolean is(String type) {
        if (type.equals("orgAdmin")) {
            return true;
        }
        return super.is(type);
    }
}
