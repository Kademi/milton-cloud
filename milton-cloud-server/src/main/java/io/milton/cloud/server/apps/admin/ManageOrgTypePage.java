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

import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.DeletableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.OrgType;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;

/**
 * Resource which represents a group role attached to a group
 *
 * @author brad
 */
public class ManageOrgTypePage extends AbstractResource implements DeletableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageOrgTypePage.class);
    private final ManageOrgTypesFolder parent;
    private final OrgType orgType;
    private JsonResult jsonResult;

    public ManageOrgTypePage(ManageOrgTypesFolder parent, OrgType orgType) {
        this.orgType = orgType;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            _(DataBinder.class).populate(orgType, parameters);
            session.save(orgType);
            jsonResult = new JsonResult(true, "Updated");
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            log.error("ex", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult == null) {
            jsonResult = new JsonResult(true);
            Map<String, Object> map = new HashMap<>();
            map.put("name", orgType.getName());
            map.put("displayName", orgType.getDisplayName());
            jsonResult.setData(map);
        }
        jsonResult.write(out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getTitle() {
        return orgType.getDisplayName();
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
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_ACL;
    }

    @Override
    public String getName() {
        return orgType.getName();
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        orgType.delete(session);
        tx.commit();
    }
}
