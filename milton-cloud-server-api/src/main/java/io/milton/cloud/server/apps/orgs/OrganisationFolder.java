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
package io.milton.cloud.server.apps.orgs;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.AppControl;
import io.milton.http.*;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 *
 * @author brad
 */
public class OrganisationFolder extends AbstractResource implements CommonCollectionResource, GetableResource, PropFindableResource, DeletableCollectionResource, MakeCollectionableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationFolder.class);
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private ResourceList children;
    private Map<String, String> fakeSettings;

    public OrganisationFolder(CommonCollectionResource parent, Organisation organisation) {
        this.parent = parent;
        this.organisation = organisation;
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (organisation.getWebsites() != null) {
            for (Website w : organisation.getWebsites()) {
                for (Branch b : w.getBranches()) {
                    for (AppControl ac : AppControl.find(b, session)) {
                        session.delete(ac);
                    }
                }

                w.delete(session);
            }
            organisation.setWebsites(null);
        }
        for (AppControl ac : AppControl.find(organisation, session)) {
            session.delete(ac);
        }

        organisation.delete(session);

        tx.commit();
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        Repository r = getOrganisation().createRepository(newName, currentUser, session);
        tx.commit();
        return new RepositoryFolder(this, r);
    }

    @Override
    public String getName() {
        return organisation.getName();
    }

    @Override
    public Date getModifiedDate() {
        return organisation.getModifiedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = _(ApplicationManager.class).getPage(this, childName);
        if (r != null) {
            return r;
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (organisation.getRepositories() != null) {
                for (Repository repo : organisation.getRepositories()) {
                    RepositoryFolder rf = new RepositoryFolder(this, repo);
                    children.add(rf);
                }
            }
            children.add(new OrganisationsFolder("organisations", this, organisation));

            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveId("menuDashboard");
        _(HtmlTemplater.class).writePage("admin", "admin/dashboard", this, params, out);
    }

    public String getTitle() {
        return "Dashboard";
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
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Date getCreateDate() {
        return organisation.getCreatedDate();
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
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

    @Override
    public boolean isLockedOutRecursive(Request request) {
        return false;
    }

    /**
     * Hardcoded settings to make themes work when editing in the admin console
     *
     * @return
     */
    public Map<String, String> getSettings() {
        if (fakeSettings == null) {
            fakeSettings = new HashMap<>();
            fakeSettings.put("heroColour1", "#88c03f");
            fakeSettings.put("heroColour2", "#88c03f");
            fakeSettings.put("textColour1", "#1C1D1F");
            fakeSettings.put("textColour2", "#2F2F2F");
        }
        return fakeSettings;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }

    @Override
    public boolean is(String type) {
        if( type.equals("organisation")) {
            return true;
        }
        return super.is(type);
    }
    
    
}
