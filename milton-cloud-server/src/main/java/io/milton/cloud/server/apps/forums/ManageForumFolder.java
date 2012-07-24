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
import io.milton.cloud.server.db.ForumTopic;
import io.milton.cloud.server.web.*;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.principal.Principal;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * for managing a single forum
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageForumFolder extends AbstractCollectionResource implements PropertySourcePatchSetter.CommitableResource, PostableResource, DeletableCollectionResource {

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
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        String newName = parameters.get("newName");
        if(newName != null ) {
            createTopic(newName, tx, session);
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
    
    
    /**
     * Implemented to support saving after proppatch
     *
     * @param knownProps
     * @param errorProps
     */
    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Response.Status, List<PropFindResponse.NameAndError>> errorProps) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        session.save(forum);
        tx.commit();
    }

    public String getTitle() {
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
            List<ForumTopic> topics = forum.getForumTopics();
            if( topics != null ) {
                for( ForumTopic t : topics ) {
                    ManageTopicFolder ftaf = new ManageTopicFolder(t, this);
                    children.add(ftaf);
                }
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
        return parent.getOwner();
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
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
    
    private void createTopic(String newTitle, Transaction tx, Session session) {
        String newName = NewPageResource.findAutoName(newTitle, this, null);
        newName = newName.replace(".html", ""); // HACK: remove .html suffix added about
        ForumTopic t = new ForumTopic();
        t.setForum(forum);
        t.setTitle(newTitle);
        t.setName(newName);
        session.save(t);
        tx.commit();
        jsonResult = new JsonResult(true, "Created", newName);
    }

    @Override
    public boolean isLockedOutRecursive(Request request) {
        return false;
    }


}
