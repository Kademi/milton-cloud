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
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Organisation;
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
import io.milton.http.Request;
import io.milton.http.Request.Method;
import java.util.HashMap;

/**
 * Represents the collection of users defined on this organisation as their
 * admin org
 *
 * @author brad
 */
public class UsersFolder extends AbstractCollectionResource implements GetableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UsersFolder.class);
    private final CommonCollectionResource parent;
    private final String name;
    private ResourceList children;
    private Map<String, UserResource> tempChildren; // used to hold references to children when the whole collection has not been loaded

    public UsersFolder(CommonCollectionResource parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("admin", "user/home", this, params, out);
    }

    public Resource findUserResource(Profile p) {
        if( p == null) {
            return  null;
        }
        if (children != null) {
            return NodeChildUtils.childOf(children, p.getName());
        } else {
            if (tempChildren == null) {
                tempChildren = new HashMap<>();
            }
            UserResource r = tempChildren.get(p.getName());
            if (r == null) {
                r = new UserResource(this, p);
                tempChildren.put(p.getName(), r);
            }
            return r;
        }
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            // Attempt to locate a user without loading entire collection
            Profile p = Profile.find(childName, SessionManager.session());
            return findUserResource(p);
        } else {
            return super.child(childName); // will scan the list of loaded children
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
            List<Profile> list = Profile.findByBusinessUnit(getOrganisation(), SessionManager.session());
            for (Profile e : list) {
                UserResource r;
                if( tempChildren != null && tempChildren.containsKey(e.getName())) {
                    r = tempChildren.get(e.getName());
                } else {
                    r = new UserResource(this, e);
                }
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
    public String getName() {
        return name;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        if( method.equals(Method.PROPFIND)) {
            return auth != null && auth.getTag() != null; // allow browsing of users folder
        }
        return super.authorise(request, method, auth);
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
        return Priviledge.READ_CONTENT;
    }      
    
}