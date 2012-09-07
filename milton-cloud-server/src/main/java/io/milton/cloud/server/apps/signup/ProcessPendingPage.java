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
package io.milton.cloud.server.apps.signup;

import io.milton.cloud.server.db.GroupMembershipApplication;
import io.milton.cloud.server.web.*;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For handling AJAX requests to enable or disable membership applications
 *
 * @author brad
 */
public class ProcessPendingPage extends AbstractResource implements PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ProcessPendingPage.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final SignupApp app;
    private JsonResult jsonResult;

    public ProcessPendingPage(String name, CommonCollectionResource parent, SignupApp app) {
        this.parent = parent;
        this.name = name;
        this.app = app;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("applicationId")) {
            String sAppId = parameters.get("applicationId");
            String sEnabled = parameters.get("enable");
            Long appId = Long.parseLong(sAppId);
            Boolean b = Boolean.parseBoolean(sEnabled);
            GroupMembershipApplication gma = (GroupMembershipApplication) session.get(GroupMembershipApplication.class, appId);
            app.processPending(gma, b, session);
            tx.commit();
            jsonResult = new JsonResult(true);
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        if (jsonResult != null) {
            return "application/x-javascript; charset=utf-8";
        }
        return "text/html";
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

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }
}
