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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.server.apps.website.AppControlBean;
import io.milton.cloud.server.apps.website.AppsPage;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.http.Auth;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Website;

/**
 * Should be created with a parent of either an OrganisationFolder
 *
 * @author brad
 */
public abstract class AbstactAppsPage extends AbstractResource implements GetableResource, PostableResource, AppsPage {

    private static final Logger log = LoggerFactory.getLogger(AbstactAppsPage.class);
    protected final String name;
    protected final CommonCollectionResource parent;
    protected final Organisation organisation;
    protected final Branch websiteBranch; // optional
    protected JsonResult jsonResult;
    protected ApplicationManager appManager;

    public AbstactAppsPage(String name, Organisation organisation, CommonCollectionResource parent, Branch websiteBranch) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
        this.websiteBranch = websiteBranch;
        appManager = _(ApplicationManager.class);        
    }

      

    @Override
    public boolean hasSettings(AppControlBean appBean) {
        Application app = appManager.get(appBean.getAppId());
        return (app instanceof SettingsApplication);
    }

    @Override
    public String getSummary(String appId) {
        Application app = appManager.get(appId);
        if (app != null) {
            return app.getSummary(organisation, websiteBranch);
        } else {
            return "Unknown app: " + appId;
        }
    }
    
    @Override
    public String getTitle(String appId) {
        Application app = appManager.get(appId);
        if (app != null) {
            return app.getTitle(organisation, websiteBranch);
        } else {
            return "Unknown app: " + appId;
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
    public Organisation getOrganisation() {
        return organisation;
    }

    public List<Organisation> getChildOrganisations() {
        return getOrganisation().childOrgs();
    }

    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }    
}
