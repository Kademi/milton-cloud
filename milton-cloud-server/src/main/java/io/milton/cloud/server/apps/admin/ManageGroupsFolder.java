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

import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.resource.CollectionResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageGroupsFolder extends AbstractResource implements GetableResource, MakeCollectionableResource, CommonCollectionResource, TitledPage {

    private static final Logger log = LoggerFactory.getLogger(ManageGroupsFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private JsonResult jsonResult;
    private ResourceList children;

    public ManageGroupsFolder(String name, Organisation organisation, CommonCollectionResource parent) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }
    
    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        log.info("createCollection: " + newName);
        if( newName.equals("users")) {
            throw new RuntimeException("Cannot use reserved name 'users'");
        }
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Group group = getOrganisation().createGroup(newName);
        group.setRegistrationMode(Group.REGO_MODE_ADMIN_REVIEW);
        session.save(group);
        tx.commit();
        return new ManageGroupFolder(this, group);
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ResourceList();
            for( Group g : getGroups() ) {
                ManageGroupFolder mgf = new ManageGroupFolder(this,g);
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
            MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuGroups");
            _(HtmlTemplater.class).writePage("admin", "admin/manageGroups", this, params, out);
        }
    }

    public List<Group> getGroups() {
        return Group.findByOrg(getOrganisation(), SessionManager.session());
    }

    public boolean isSelected(Group g, String role) {
        if( g.getGroupRoles() == null ) {
            return false;
        }
        for( GroupRole gr : g.getGroupRoles()) {
            if( gr.getRoleName().contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    public List<String> getAllRoles() {
        List<String> names = new ArrayList<>();
        for( Role r : _(SpliffySecurityManager.class).getGroupRoles() ) {
            names.add(r.getName());
        }
        Collections.sort(names);
        return names;
    }

    @Override
    public String getTitle() {
        return "Manage groups";
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
        if (type.equals("groupsAdmin")) {
            return true;
        }
        return super.is(type);
    }
}
