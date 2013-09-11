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
package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.db.Forum;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.web.*;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.annotations.BeanPropertyResource;
import io.milton.cloud.server.web.templating.DataBinder;
import static io.milton.context.RequestContext._;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * for managing a single forum
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageForumFolder extends AbstractCollectionResource implements DeletableCollectionResource, GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageForumFolder.class);
    
    private final Forum forum;
    private final CommonCollectionResource parent;
    private ResourceList children;

    private JsonResult jsonResult;
    
    public ManageForumFolder(Forum forum, CommonCollectionResource parent) {        
        this.forum = forum;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        try {
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            _(DataBinder.class).populate(forum, parameters);
            // Check if uniquely named
            for( Forum existing: Forum.findByWebsite(forum.getWebsite(), session) ) {
                if( existing.getName().equals( forum.getName() ) && existing.getId() != forum.getId() ) {
                    jsonResult = new JsonResult(false, "Please choose a unique name");
                    return null;
                }
            }
            session.save(forum);
            tx.commit();
            jsonResult = new JsonResult(true);
        } catch (Exception e) {
            log.error("Exception updating forum: " + forum.getName(), e);
            jsonResult = new JsonResult(false);
        }
        return null;
    }

    

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if( jsonResult != null ) {
            jsonResult.write(out);
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        forum.delete(session);
        tx.commit();
    }    
    
    

    public List<ForumPost> getRecentPosts() {
        return ForumPost.findRecentByForum(forum, 10, SessionManager.session());  
        
    }
    
    public String getTitle() {
        if( forum.getTitle() == null ) {
            return forum.getName();
        }
        return forum.getTitle();
    }

    public void setTitle(String t) {
        forum.setTitle(t);
    }

    public String getNotes() {
        return forum.getNotes();
    }

    public void setNotes(String s) {
        forum.setNotes(s);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
        }
        return children;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public String getName() {
        return forum.getName();
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
    public boolean isLockedOutRecursive(Request request) {
        return false;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }    
}
