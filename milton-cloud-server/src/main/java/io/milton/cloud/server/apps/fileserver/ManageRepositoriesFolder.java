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
package io.milton.cloud.server.apps.fileserver;

import io.milton.cloud.server.apps.admin.*;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Collections;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageRepositoriesFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsitesFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private ResourceList children;
    private JsonResult jsonResult;

    public ManageRepositoriesFolder(String name, Organisation organisation, CommonCollectionResource parent) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (curUser == null) {
            log.warn("req: " + HttpManager.request());
            throw new RuntimeException("No current user!!");
        }

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        String newName = parameters.get("newName");
        if( newName == null ) {
            jsonResult = JsonResult.fieldError("newName", "Please enter a name to create");
        } else {
            Organisation org = getOrganisation();
            if( parent.child(newName) != null ) {
                jsonResult = JsonResult.fieldError("newName", "There is already a resource with that name, please choose another");
            } else {
                Repository r = org.createRepository(newName, curUser, session);
                RepositoryFolder rf = new RepositoryFolder(parent, r, true);
                log.info("created: " + r.getName());
                jsonResult = new JsonResult(true, "Created ok", rf.getHref());
            }
        }
        
        tx.commit();

        return null;
    }

    
    
    public String getTitle() {
        return "Manage Repositories";
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "fileserver/manageRepos", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for (Repository w : getRepositories()) {
                //ManageWebsitePage p = new ManageWebsitePage(w, this);
                //children.add(p);
            }
        }
        return children;
    }

    @Override
    public boolean isDir() {
        return true;
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
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }
    
    public List<Repository> getRepositories() {
        List<Repository> list = getOrganisation().getRepositories();
        if( list == null ) {
            list = Collections.EMPTY_LIST;
        }
        return list;
    }

}
