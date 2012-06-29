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
package io.milton.cloud.server.apps.user;


import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.UserResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Represents the collection of users defined on this organisation as their admin
 * org
 *
 * @author brad
 */
public class UsersFolder extends AbstractCollectionResource implements GetableResource {
    private final CommonCollectionResource parent;
    private final String name;
    
    private ResourceList children;

    public UsersFolder(CommonCollectionResource parent, String name) {
        super(parent.getServices());
        this.parent = parent;
        this.name = name;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        services.getHtmlTemplater().writePage("user/home", this, params, out);
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return super.child(childName);
    }
    
    
        
    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return getParent().getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        getParent().addPrivs(list, user);
    }

    @Override
    public String getName() {
        return name;
    }



    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ResourceList();
            services.getApplicationManager().addBrowseablePages(this, children);
            List<Profile> users = Profile.findByAdminOrg(getOrganisation(), SessionManager.session());
            for( Profile p : users ) {
                UserResource ur = new UserResource(this, p, services.getApplicationManager());
                children.add(ur);
            }
        }
        return children;
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
    public String getContentType(String accepts) {
        return "text/html";
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