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
package io.milton.cloud.server.apps.website;

import io.milton.vfs.db.Website;
import io.milton.vfs.db.Organisation;
import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.util.*;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.web.*;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;

import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents the root of a website. A "website" in this context is a product,
 * its a customer facing side of some activity, such as Learning Management
 * System or business website.
 *
 * Resources within a WebsiteRootFolder will often behave differently then if
 * they were located under a OrganisationRootFolder, because the assumption is
 * that websites are for customers, while aadministrators will accessing the
 * organisation directly
 * 
 * While WebsiteRootFolder is a view of a repository (or rather a branch within a repository)
 * it also provides access to other repositories. Any repository within the containing
 * organisation can be access by name from the root folder. For example, if an org
 * has this structure:
 * 
 * myOrg
 *  - milton.io
 *  - maven
 * 
 * ... then requests to http://milton.io/maven will show the maven repository
 *
 * @author brad
 */
public class WebsiteRootFolder extends BranchFolder implements RootFolder, CommonCollectionResource, GetableResource, PropFindableResource {

    private final ApplicationManager applicationManager;
    private final Website website;
    private Map<String, Object> attributes;

    public WebsiteRootFolder(ApplicationManager applicationManager, Website website, Branch branch) {
        super("", null, branch);
        this.website = website;
        this.applicationManager = applicationManager;
    }

    @Override
    protected void renderPage(OutputStream out, Map<String, String> params) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        RenderFileResource index = getIndex();
        if( index != null ) {
            index.sendContent(out, null, params, "text/html");
        } else {
            super.renderPage(out, params);
        }
    }

    
    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getId() {
        return website.getName() + "_" + branch.getName();
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (method.equals(Method.PROPFIND)) { // force login for webdav browsing
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
        Resource r = applicationManager.getPage(this, childName);
        if (r != null) {
            return r;
        }
        r = NodeChildUtils.childOf(getChildren(), childName);
        if (r != null) {
            return r;
        }
        // Check for a repository of the organisation
        Repository repo = website.getOrganisation().repository(childName);
        if( repo != null ) {
            Branch b = repo.liveBranch();
            if( b != null ) {
                BranchFolder bf = new BranchFolder(childName, this, b);
                children.add(bf);
                return bf;
            }
        }
        
        return r;
    }

    @Override
    public PrincipalResource findEntity(Profile u) throws NotAuthorizedException, BadRequestException {
        return UserApp.findEntity(u, this);
    }


    @Override
    public Organisation getOrganisation() {
        return (Organisation) website.getOrganisation();
    }

    public Website getWebsite() {
        return website;
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }


    @Override
    public String getEmailAddress() {
        String fromAddress = "noreply@";
        String d = website.getDomainName();
        if (d.startsWith("www")) {
            d = d.replace("www.", "");
        }
        fromAddress += d;
        return fromAddress;
    }

    @Override
    public boolean is(String type) {
        if( type.equals("website")) {
            return true;
        }
        return super.is(type);
    }
    
    
}
