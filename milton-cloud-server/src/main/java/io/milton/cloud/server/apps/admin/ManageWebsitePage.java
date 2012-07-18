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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageWebsitePage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsitePage.class);
    private final Website website;
    private final CommonCollectionResource parent;
    private JsonResult jsonResult;

    public ManageWebsitePage(Website website, CommonCollectionResource parent) {
        this.parent = parent;
        this.website = website;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        for (AppControlBean a : getApps()) {
            boolean enabled = parameters.containsKey(a.getAppId());
            setStatus(a.getAppId(), enabled, session);
        }
        tx.commit();
        jsonResult = new JsonResult(true);
        return null;
    }

    public List<AppControlBean> getApps() {
        ApplicationManager appManager = _(ApplicationManager.class);
        List<Application> activeApps;
        activeApps = appManager.findActiveApps(website);
        List<AppControlBean> beans = new ArrayList<>();
        for (Application app : appManager.getApps()) {
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
        AppControl.setStatus(appId, website, enabled, currentUser, currentDate, session);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "admin/manageWebsite", this, params, out);
        }
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return website.getName();
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
    public Map<Principal, List<Priviledge>> getAccessControlList() {
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
        return parent.getOrganisation();
    }

    public Website getWebsite() {
        return website;
    }
    
    public List<GroupInWebsite> getGroupsInWebsite() {
        return website.groups(SessionManager.session());
    }
    
}
