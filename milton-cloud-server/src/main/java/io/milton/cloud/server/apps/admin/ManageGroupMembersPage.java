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

import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.vfs.db.GroupMembership;
import java.util.ArrayList;
import java.util.List;

/**
 * Resource which represents a group role attached to a group
 *
 * @author brad
 */
public class ManageGroupMembersPage extends AbstractResource implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageGroupMembersPage.class);
    private final ManageGroupFolder parent;
    private final String name;
    private JsonResult jsonResult;
    private List<PrincipalResource> members = null;

    public ManageGroupMembersPage(ManageGroupFolder parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "admin/manageGroupMembers", this, params, out);
        }
    }

    public List<PrincipalResource> getMembers() throws NotAuthorizedException, BadRequestException {
        if (members == null) {
            members = new ArrayList<>();
            if (parent.getGroup().getGroupMemberships() != null) {
                RootFolder root = _(CurrentRootFolderService.class).getRootFolder();
                for (GroupMembership m : parent.getGroup().getGroupMemberships()) {
                    PrincipalResource r = UserApp.findEntity(m.getMember(), root);
                    members.add(r);
                }
            }
        }
        return members;
    }

    public String getTitle() {
        return "Manage members for group: " + parent.getName();
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
}
