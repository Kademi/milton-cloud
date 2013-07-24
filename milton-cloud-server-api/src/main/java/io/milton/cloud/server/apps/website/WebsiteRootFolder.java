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
import io.milton.cloud.server.manager.CommentService;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;

import io.milton.http.exceptions.NotFoundException;
import io.milton.property.BeanProperty;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;

import static io.milton.context.RequestContext._;
import io.milton.property.BeanPropertyResource;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
 * While WebsiteRootFolder is a view of a repository (or rather a branch within
 * a repository) it also provides access to other repositories. Any repository
 * within the containing organisation can be access by name from the root
 * folder. For example, if an org has this structure:
 *
 * myOrg - milton.io - maven
 *
 * ... then requests to http://milton.io/maven will show the maven repository
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class WebsiteRootFolder extends BranchFolder implements RootFolder, CommonCollectionResource, GetableResource, PropFindableResource, WebsiteResource {

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
        GetableResource index = getIndex();
        if (index != null) {
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
    public String getDomainName() {
        return branch.getName() + "." + website.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
    }

    
    
    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (request.getMethod().equals(Method.PROPFIND)) { // force login for webdav browsing
            return _(SpliffySecurityManager.class).getCurrentUser() != null;
        }
        return super.authorise(request, method, auth);
    }

    @Override
    public Date getModifiedDate() {
        Commit c = branch.getHead();
        if( c == null ) {
            return branch.getCreatedDate();            
        } else {
            return c.getCreatedDate();
        }
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
        if (repo != null) {
            Branch b = repo.liveBranch();
            if (b != null) {
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

    @Override
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
        if (d == null) {
            d = website.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
        } else {
            if (d.startsWith("www")) {
                d = d.replace("www.", "");
            }
        }
        fromAddress += d;
        return fromAddress;
    }

    @Override
    public boolean is(String type) {
        if (type.equals("website")) {
            return true;
        }
        return super.is(type);
    }
    
    public String getThemeName() {
        return branch.getPublicTheme();
    }
    

    public List<CommentBean> getComments() {
        return _(CommentService.class).comments(this);
    }

    public int getNumComments() {
        List<CommentBean> list = getComments();
        if (list == null) {
            return 0;
        } else {
            return list.size();
        }
    }

    public void setNewComment(String s) throws NotAuthorizedException {
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
            Transaction tx = SessionManager.beginTx();
            _(CommentService.class).newComment(this, s, wrf.getWebsite(), currentUser, SessionManager.session()); 
            tx.commit();
        }
    }

    /**
     * This is just here to make newComment a bean property
     *
     * @return
     */
    @BeanProperty(writeRole = Priviledge.READ)
    public String getNewComment() {
        return null;
    }    
}
