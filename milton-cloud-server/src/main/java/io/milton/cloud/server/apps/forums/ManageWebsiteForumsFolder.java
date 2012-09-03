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
package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.db.Forum;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.resource.Resource;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Provides a list of forums per website
 *
 * @author brad
 */
public class ManageWebsiteForumsFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsiteForumsFolder.class);
    private final ManageForumsFolder parent;
    private final Organisation organisation;
    private final Website website;
    private ResourceList children;
    private JsonResult jsonResult;

    /**
     *
     * @param name
     * @param organisation
     * @param parent
     * @param website - null if none selected
     */
    public ManageWebsiteForumsFolder(Organisation organisation, ManageForumsFolder parent, Website website) {
        this.organisation = organisation;
        this.parent = parent;
        this.website = website;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<Forum> forums = Forum.findByWebsite(website, SessionManager.session());
            for (Forum w : forums) {
                ManageForumFolder p = new ManageForumFolder(w, this);
                children.add(p);
            }
        }
        return children;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        String newName = parameters.get("newName");
        if (newName != null) {
            createForum(newName, tx, session);
        }
        return null;
    }
    
    public ManageForumsFolder getForumsRoot() {
        return parent;
    }
    

    public String getTitle() {
        return "Manage forums: " + website.getDomainName();
    }

    public List<Website> getWebsites() {
        return parent.getWebsites();
    }

    public Website getWebsite() {
        return website;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuManageForums", "menuEditForums");
            _(HtmlTemplater.class).writePage("admin", "forums/manageForums", this, params, out);
        }
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
        return website.getDomainName();
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
    public boolean is(String type) {
        if (type.equals("manageRewards")) {
            return true;
        }
        return super.is(type);
    }

    private void createForum(String newTitle, Transaction tx, Session session) {
        Forum f = new Forum();
        String newName = NewPageResource.findAutoName(newTitle, this, null);
        newName = newName.replace(".html", ""); // HACK: remove .html suffix added about
        Forum.addToWebsite(website, newName, newTitle, session);
        tx.commit();
        jsonResult = new JsonResult(true, "Created", newName);
    }
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }        
}
