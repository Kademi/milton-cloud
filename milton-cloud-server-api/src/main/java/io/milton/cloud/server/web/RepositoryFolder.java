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
package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.http.Auth;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.*;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;

/**
 * A RepositoryFolder just holds the branches of the folder
 *
 * @author brad
 */
@BeanPropertyResource(value="milton")
public class RepositoryFolder extends AbstractCollectionResource implements PropFindableResource, MakeCollectionableResource, GetableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BranchFolder.class);
    protected final boolean renderMode;
    protected final CommonCollectionResource parent;
    protected final Repository repo;
    protected ResourceList children;
    

    public RepositoryFolder(CommonCollectionResource parent, Repository r, boolean renderMode) {
        this.renderMode = renderMode;
        this.repo = r;
        this.parent = parent;
    }

    @Override
    public Resource child(String childName) {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public ResourceList getChildren() {
        if (children == null) {
            children = new ResourceList();
            for( Branch b : repo.getBranches()) {
                BranchFolder bf = new BranchFolder(b.getName(), this, b, renderMode);
                children.add(bf);
            }
            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    public String getTitle() {
        return repo.getTitle();
    }
    
    public String getNotes() {
        return repo.getNotes();
    }

    public Repository getRepository() {
        return repo;
    }
    
    
    
    @Override
    public String getName() {
        return repo.getName();
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        log.trace("createCollection: " + newName);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        
        Branch b = repo.createBranch(newName, _(SpliffySecurityManager.class).getCurrentUser(), session);
        BranchFolder bf = new BranchFolder(newName, this, b, false);
        
        tx.commit();
        return bf;
    }

    

    @Override
    public Date getCreateDate() {
        return repo.getCreatedDate();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {

    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {

        String type = HttpManager.request().getParams().get("type");
        if (type == null || type.length() == 0) {
            return "text/html";
        } else {
            if (type.equals("hashes") || type.equals("revision")) {
                return "text/plain";
            } else {
                return type;
            }
        }
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
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    /**
     * Get all allowed priviledges for all principals on this resource. Note
     * that a principal might be a user, a group, or a built-in webdav group
     * such as AUTHENTICATED
     *
     * @return
     */
    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }


    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
