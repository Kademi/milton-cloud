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


import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.templating.HtmlTemplater;
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

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.*;

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
    private InstantiationVisitor instantiationVisitor;

    public UsersFolder(CommonCollectionResource parent, String name) {        
        this.parent = parent;
        this.name = name;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("admin","user/home", this, params, out);
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return super.child(childName); // TODO: need to pick out resources without loading whole collection
    }
    
    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
            List<BaseEntity> entities = BaseEntity.find(getOrganisation(), SessionManager.session());
            for( BaseEntity e : entities ) {
                CommonResource r = instantiateEntity(e);
                children.add(r);
            }
        }
        return children;
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

    /**
     * Uses the visitor pattern to create an instance of an appropriate resource
     * 
     * @param e
     * @return 
     */
    private CommonResource instantiateEntity(BaseEntity e) {
        if( instantiationVisitor == null ) {
            instantiationVisitor = new InstantiationVisitor();
        }
        instantiationVisitor.visit(e);
        return instantiated;
    }
    
    private CommonResource instantiated;
    
    private class InstantiationVisitor extends AbstractVfsVisitor {

        @Override
        public void visit(Group r) {
            instantiated = new GroupResource(UsersFolder.this, r);
        }

        @Override
        public void visit(Organisation p) {
            instantiated = new OrganisationFolder(UsersFolder.this, p);
        }

        @Override
        public void visit(Profile r) {
            instantiated = new UserResource(UsersFolder.this, r);
        }
        
    }
}