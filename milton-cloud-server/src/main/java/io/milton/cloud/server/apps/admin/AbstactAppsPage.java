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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Website;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Should be created with a parent of either an OrganisationFolder
 *
 * @author brad
 */
public abstract class AbstactAppsPage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(AbstactAppsPage.class);
    protected final String name;
    protected final CommonCollectionResource parent;
    protected final Organisation organisation;
    protected final Website website; // optional
    protected JsonResult jsonResult;
    protected ApplicationManager appManager;

    public AbstactAppsPage(String name, Organisation organisation, CommonCollectionResource parent, Website website) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
        this.website = website;
        appManager = _(ApplicationManager.class);
    }

      

    public boolean hasSettings(AppControlBean appBean) {
        Application app = appManager.get(appBean.getAppId());
        return (app instanceof SettingsApplication);
    }

    public String getSummary(String appId) {
        Application app = appManager.get(appId);
        if (app != null) {
            return app.getSummary(organisation, website);
        } else {
            return "Unknown app: " + appId;
        }
    }
    
    public String getTitle(String appId) {
        Application app = appManager.get(appId);
        if (app != null) {
            return app.getTitle(organisation, website);
        } else {
            return "Unknown app: " + appId;
        }        
    }

    public List<AppControlBean> getApps() {
        List<Application> availableApps;
        List<Application> activeApps;
        if (website != null) {
            availableApps = appManager.findActiveApps(website.getOrganisation());
            activeApps = appManager.findActiveApps(website);
        } else {
            if (organisation.getOrganisation() == null) {
                availableApps = appManager.getApps(); // all of them
            } else {
                availableApps = appManager.findActiveApps(organisation.getOrganisation()); // from parent
            }
            activeApps = appManager.findActiveApps(organisation);
        }
        List<AppControlBean> beans = new ArrayList<>();
        for (Application app : availableApps) {
            boolean enabled = isEnabled(app, activeApps);
            AppControlBean bean = new AppControlBean(app.getInstanceId(), enabled);
            beans.add(bean);
        }
        return beans;
    }

    private boolean isEnabled(Application app, List<Application> activeApps) {
        return activeApps.contains(app);
    }

    private void setStatus(String appId, boolean enabled, Session session) {
        log.info("setStatus: " + appId + " = " + enabled);
        Date currentDate = _(CurrentDateService.class).getNow();
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (website != null) {
            AppControl.setStatus(appId, website, enabled, currentUser, currentDate, session);
        } else {
            AppControl.setStatus(appId, organisation, enabled, currentUser, currentDate, session);
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
        List<Organisation> list = new ArrayList<>();
        List<BaseEntity> members = getOrganisation().getMembers();
        if (members == null || members.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        for (BaseEntity be : members) {
            if (be instanceof Organisation) {
                list.add((Organisation) be);
            }
        }
        return list;
    }

    protected void updateApplicationSettings(Map<String, String> parameters, Map<String, FileItem> files, Transaction tx) throws NotAuthorizedException, HibernateException, BadRequestException, ConflictException {
        // Updating the settings for an application
        String appId = parameters.get("settingsAppId");
        Application app = appManager.get(appId);
        if (app instanceof SettingsApplication) {
            SettingsApplication settingsApp = (SettingsApplication) app;
            jsonResult = settingsApp.processForm(parameters, files, organisation, website);
            if (jsonResult.isStatus()) {
                tx.commit();
            } else {
                tx.rollback();
            }
        } else {
            tx.rollback();
            jsonResult = new JsonResult(false, "Application does not support settings");
        }
    }

    protected void updateApplicationEnabled(Map<String, String> parameters, Session session, Transaction tx) throws HibernateException {
        // Enabling or disabling an app
        String appId = parameters.get("appId");
        String sEnabled = parameters.get("enabled");
        Boolean isEnabled = Boolean.parseBoolean(sEnabled);
        setStatus(appId, isEnabled, session);
        tx.commit();
        jsonResult = new JsonResult(true);
    }
}
