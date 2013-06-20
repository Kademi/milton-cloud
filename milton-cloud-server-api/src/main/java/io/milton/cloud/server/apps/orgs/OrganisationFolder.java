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
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
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
public class OrganisationFolder extends AbstractResource implements CommonCollectionResource, GetableResource, PropFindableResource, DeletableCollectionResource, MakeCollectionableResource, PostableResource, TitledPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationFolder.class);
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private ResourceList children;
    private Map<String, String> fakeSettings;
    private JsonResult jsonResult;

    public OrganisationFolder(CommonCollectionResource parent, Organisation organisation) {
        this.parent = parent;
        this.organisation = organisation;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            _(DataBinder.class).populate(organisation, parameters);            
            String s = WebUtils.getRawParam(parameters, "orgTypeName");
            OrgType orgType = null;
            if( s != null ) {
                orgType = findOrgType(s);
            }
            organisation.setOrgType(orgType);
            session.save(organisation);
            tx.commit();
            jsonResult = new JsonResult(true);
        } catch (Exception e) {
            tx.rollback();
            log.error("ex", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }
    
    public OrgType findOrgType(String name) {
        Organisation o = organisation;
        while( o != null ) {
            OrgType ot = o.orgType(name);
            if( ot != null ) {
                return ot;
            }
            o = o.getOrganisation();
        }
        return null;
    }

    /**
     * Instead of actually deleting, we set the hiddn flag and give it a name
     * and orgId like originalname-deleted-1234 (where 1234 is the timestamp)
     *
     * Any contained websites have their domain names set to blank, and their
     * names modified to prevent conflicting values
     *
     * Any contained org's have their orgId's modified to prevent conflicting
     * values
     *
     *
     * @throws NotAuthorizedException
     * @throws ConflictException
     * @throws BadRequestException
     */
    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        organisation.softDelete(session);

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
        return organisation.getOrgId();
    }

    public String getDisplayName() {
        if (organisation.getTitle() != null && organisation.getTitle().length() > 0) {
            return organisation.getTitle();
        } else {
            return organisation.getOrgId();
        }
    }

    @Override
    public Date getModifiedDate() {
        return organisation.getModifiedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        if (childName.equals("viewDetails")) {
            return new ViewOrgPage(childName, this);
        }
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

            children.add(new RepositoriesFolder("repositories", this));
            children.add(new OrganisationsFolder("organisations", this, organisation));

            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult == null && "application/json".equals(contentType)) {            
            Map<String,Object> map = new HashMap<>();
            map.put("orgId", organisation.getOrgId());
            map.put("adminDomain", organisation.getAdminDomain() );
            map.put("title", organisation.getTitle());
            map.put("address", organisation.getAddress());
            map.put("addressLine2", organisation.getAddressLine2());
            map.put("addressState", organisation.getAddressState());
            map.put("postcode", organisation.getPostcode());
            map.put("phone", organisation.getPhone());
            if( organisation.getOrgType() != null ) {
                map.put("orgTypeName", organisation.getOrgType().getName());
            }
            jsonResult = new JsonResult(true);
            jsonResult.setData(map);
        }

        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveId("menuDashboard");
            _(HtmlTemplater.class).writePage("admin", "admin/dashboard", this, params, out);
        }
    }

    @Override
    public String getContentType(String accepts) {
        if (accepts != null && accepts.contains("application/json")) {
            return "application/json";
        }
        return super.getContentType(accepts);
    }

    @Override
    public String getTitle() {
        return getOrganisation().getFormattedName() + " Dashboard";
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
        if (type.equals("organisation")) {
            return true;
        }
        return super.is(type);
    }
}

