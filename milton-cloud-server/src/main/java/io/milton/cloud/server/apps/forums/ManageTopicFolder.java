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

import io.milton.cloud.server.db.ForumTopic;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.principal.Principal;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageTopicFolder extends AbstractCollectionResource implements PropertySourcePatchSetter.CommitableResource, DeletableCollectionResource {

    private final ForumTopic forumTopic;
    private final CommonCollectionResource parent;
    private ResourceList children;

    public ManageTopicFolder(ForumTopic forumTopic, CommonCollectionResource parent) {        
        this.forumTopic = forumTopic;
        this.parent = parent;
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
        session.save(forumTopic);
        tx.commit();
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        forumTopic.delete(session);
        tx.commit();
    }    
    
    public String getTitle() {
        return forumTopic.getTitle();
    }

    public void setTitle(String t) {
        forumTopic.setTitle(t);
    }

    public String getNotes() {
        return forumTopic.getNotes();
    }

    public void setNotes(String s) {
        forumTopic.setNotes(s);
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
        return forumTopic.getName();
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public boolean isLockedOutRecursive(Request request) {
        return false;
    }
}
