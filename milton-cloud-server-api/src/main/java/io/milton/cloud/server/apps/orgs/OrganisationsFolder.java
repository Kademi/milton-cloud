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
package io.milton.cloud.server.apps.orgs;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.*;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class OrganisationsFolder extends AbstractResource implements CommonCollectionResource, GetableResource, PropFindableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationRootFolder.class);
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private final String name;
    private ResourceList children;

    public OrganisationsFolder(String name, CommonCollectionResource parent,  Organisation organisation) {
        
        this.name = name;
        this.parent = parent;
        this.organisation = organisation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (method.equals(Request.Method.PROPFIND)) { // force login for webdav browsing
            return _(SpliffySecurityManager.class).getCurrentUser() != null;
        }
        return true;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = _(ApplicationManager.class).getPage(this, childName);
        if( r != null ) {
            return r;
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for( Organisation o : organisation.childOrgs()) {
                OrganisationFolder of = new OrganisationFolder(this, o);
                children.add(of);
            }
            
            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        String s = super.checkRedirect(request);
        if( s != null ) {
            return s;
        }
        if( request.getMethod().equals(Method.GET)) {
            return "manage";
        }        
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
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
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

