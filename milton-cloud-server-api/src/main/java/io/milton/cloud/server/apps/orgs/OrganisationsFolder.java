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
package io.milton.cloud.server.apps.orgs;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.context.ClassNotInContextException;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import java.lang.reflect.InvocationTargetException;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class OrganisationsFolder extends AbstractResource implements CommonCollectionResource, GetableResource, PropFindableResource, PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationRootFolder.class);
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private final String name;
    private ResourceList children;
    private JsonResult jsonResult;
    private List<OrgSearchResult> searchResults;

    public OrganisationsFolder(String name, CommonCollectionResource parent, Organisation organisation) {
        this.name = name;
        this.parent = parent;
        this.organisation = organisation;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            String q = params.get("q");
            List<Organisation> orgs;
            if (q != null && q.length() > 0) {
                orgs = Organisation.search(q, getOrganisation(), null, SessionManager.session());
            } else {
                orgs = listOrgs(100);
            }
            searchResults = new ArrayList<>();
            for (Organisation org : orgs) {
                OrgSearchResult sr = new OrgSearchResult(org);
                searchResults.add(sr);
            }
            MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuOrgs");
            _(HtmlTemplater.class).writePage("admin", "admin/manageOrgs", this, params, out);
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            String newOrgId = parameters.get("orgId");
            Organisation c = getOrganisation().createChildOrg(newOrgId, session);

            _(DataBinder.class).populate(c, parameters);
            session.save(c);

            Date now = _(CurrentDateService.class).getNow();
            Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
            List<Application> avail = _(ApplicationManager.class).findAvailableApps(c);
            List<String> availAppIds = new ArrayList<>();
            for (Application app : avail) {
                availAppIds.add(app.getInstanceId());
            }
            log.info("init apps: " + avail.size());
            AppControl.initApps(availAppIds, c, curUser, now, session);

            tx.commit();
            jsonResult = new JsonResult(true, "Created", c.getOrgId());
        } catch (ClassNotInContextException | IllegalAccessException | InvocationTargetException e) {
            tx.rollback();
            log.warn("exception creating new org", e);
            jsonResult = new JsonResult(false, "Exception: " + e);
            return null;
        }

        return null;
    }

    public String getTitle() {
        return "Manage business units";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (method.equals(Request.Method.PROPFIND)) { // force login for webdav browsing
            return _(SpliffySecurityManager.class).getCurrentUser() != null;
        }
        return super.authorise(request, method, auth);
    }

    @Override
    public Date getModifiedDate() {
        return null;
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
            for (Organisation o : organisation.childOrgs()) {
                OrganisationFolder of = new OrganisationFolder(this, o);
                children.add(of);
            }
            children.add(new OrganisationsCsv("orgs.csv", this));

            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    public List<OrgSearchResult> getSearchResults() {
        return searchResults;
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
        return null;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
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
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    public String path(Organisation to) {
        if (to == null || to == getOrganisation()) {
            return "";
        } else {
            String parent = path(to.getOrganisation());
            String title = to.getTitle();
            if (title == null || title.length() == 0) {
                title = to.getOrgId();
            }
            return parent + title + "/";
        }
    }

    public String getOrgHref(Organisation to) {
        if (to == null || to == getOrganisation()) {
            return "";
        } else {
            String parent = getOrgHref(to.getOrganisation());
            return parent + to.getOrgId() + "/organisations/";
        }
    }

    public List<OrgType> getOrgTypes() {
        Organisation rootOrg = _(CurrentRootFolderService.class).getRootFolder().getOrganisation();
        return OrgType.findAllForOrg(rootOrg, SessionManager.session());
    }

    private List<Organisation> listOrgs(int limit) {
        List<Organisation> list = new ArrayList<>();
        listOrgs(limit, organisation, list);
        return list;
    }
    
    private void listOrgs(int limit, Organisation parent, List<Organisation> list) {
        for( Organisation org : parent.childOrgs()) {
            list.add(org);
            if( list.size() > limit) {
                return ;
            }
        }
        for( Organisation org : parent.childOrgs()) {
            listOrgs(limit, org, list);
            if( list.size() > limit) {
                return ;
            }
        }        
    }

    public class OrgSearchResult {

        private final Organisation organisation;

        public OrgSearchResult(Organisation organisation) {
            this.organisation = organisation;
        }

        public Organisation getOrg() {
            return organisation;
        }

        public String getId() {
            return organisation.getOrgId();
        }

        public String getTitle() {
            if (organisation.getTitle() != null && organisation.getTitle().length() > 0) {
                return organisation.getTitle();
            } else {
                return organisation.getOrgId();
            }
        }

        public String getPhone() {
            return organisation.getPhone();
        }

        public String getAddress() {
            return organisation.getAddress();
        }

        public String getParentPath() {
            return path(organisation.getOrganisation());
        }

        public String getHref() {
            return getOrgHref(organisation.getOrganisation()) + organisation.getOrgId() + "/";
        }

        public String getOrgTypeName() {
            if (organisation.getOrgType() != null) {
                return organisation.getOrgType().getName();
            } else {
                return "";
            }
        }
    }
}
