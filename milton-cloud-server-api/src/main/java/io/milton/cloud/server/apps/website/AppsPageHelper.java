/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.website;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Branch;

/**
 *
 * @author brad
 */
public class AppsPageHelper {

    private static final Logger log = LoggerFactory.getLogger(AppsPageHelper.class);
    
    private ApplicationManager appManager;

    public AppsPageHelper(ApplicationManager appManager) {
        this.appManager = appManager;
    }
    
    
    
    public JsonResult updateApplicationSettings(Organisation org, Branch websiteBranch, Map<String, String> parameters, Map<String, FileItem> files, Transaction tx) throws NotAuthorizedException, HibernateException, BadRequestException, ConflictException {
        // Updating the settings for an application
        JsonResult jsonResult;
        try {
            String appId = parameters.get("settingsAppId");
            Application app = appManager.get(appId);
            if (app instanceof SettingsApplication) {
                SettingsApplication settingsApp = (SettingsApplication) app;
                jsonResult = settingsApp.processForm(parameters, files, org, websiteBranch);
                if (jsonResult.isStatus()) {
                    tx.commit();
                } else {
                    tx.rollback();
                }
            } else {
                tx.rollback();
                jsonResult = new JsonResult(false, "Application does not support settings");
            }
        } catch (Throwable e) {
            log.error("exception saving app setting", e);
            jsonResult = new JsonResult(false, "Exception occured: " + e.getMessage());
        }
        return jsonResult;
    }

    public JsonResult updateApplicationEnabled(Organisation organisation, Branch websiteBranch, Map<String, String> parameters, Session session, Transaction tx) throws HibernateException {
        // Enabling or disabling an app
        JsonResult jsonResult;
        String appId = parameters.get("appId");
        String sEnabled = parameters.get("enabled");
        Boolean isEnabled = Boolean.parseBoolean(sEnabled);
        setStatus(organisation, websiteBranch, appId, isEnabled, session);
        tx.commit();
        jsonResult = new JsonResult(true);
        return jsonResult;
    }    
    

    public List<AppControlBean> getApps(Organisation organisation, Branch websiteBranch) {
        List<Application> availableApps;
        List<Application> activeApps;
        if (websiteBranch != null) {
            Organisation org = (Organisation) websiteBranch.getRepository().getBaseEntity();
            availableApps = appManager.findActiveApps(org);
            activeApps = appManager.findActiveApps(websiteBranch);
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

    private void setStatus(Organisation organisation, Branch websiteBranch, String appId, boolean enabled, Session session) {
        log.info("setStatus: " + appId + " = " + enabled);
        Date currentDate = _(CurrentDateService.class).getNow();
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (websiteBranch != null) {
            AppControl.setStatus(appId, websiteBranch, enabled, currentUser, currentDate, session);
        } else {
            AppControl.setStatus(appId, organisation, enabled, currentUser, currentDate, session);
        }
    }    
}
