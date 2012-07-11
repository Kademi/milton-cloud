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
package io.milton.cloud.server.apps.orgs;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.http.*;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Permission;
import io.milton.vfs.db.Profile;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;

/**
 * This is the root folder for the admin site. The admin site is used to setup
 * users and websites accessing the server
 *
 * @author brad
 */
public class OrganisationFolder extends AbstractResource implements CommonCollectionResource, GetableResource, PropFindableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationFolder.class);
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private ResourceList children;

    public OrganisationFolder(CommonCollectionResource parent, Organisation organisation) {
        this.parent = parent;
        this.organisation = organisation;
    }

    @Override
    public String getName() {
        return organisation.getName();
    }

    @Override
    public Date getModifiedDate() {
        return organisation.getModifiedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = _(ApplicationManager.class).getPage(this, childName);
        if (r != null) {
            return r;
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (organisation.getRepositories() != null) {
                for (Repository repo : organisation.getRepositories()) {
                    RepositoryFolder rf = new RepositoryFolder(this, repo, false);
                    children.add(rf);
                }
            }
            children.add(new OrganisationsFolder("organisations", this, organisation));

            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("admin","admin/dashboard", this, params, out);
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
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        Set<Permission> perms = SecurityUtils.getPermissions(user, organisation, SessionManager.session());
        SecurityUtils.addPermissions(perms, list);
        if (parent != null) {
            parent.addPrivs(list, user);
        }
    }

    @Override
    public Date getCreateDate() {
        return organisation.getCreatedDate();
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Organisation getOrganisation() {
        return organisation;
    }

    @Override
    public boolean isPublic() {
        return false;
    }
    
    
}
