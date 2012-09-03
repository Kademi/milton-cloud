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
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.RenderAppSettingsDirective;
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
import io.milton.http.Request;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageWebsitePage extends AbstactAppsPage implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsitePage.class);
    private Map<String, String> themeParams;

    public ManageWebsitePage(Website website, CommonCollectionResource parent) {
        super(website.getName(), website.getOrganisation(), parent, website);
    }

    public String getTitle() {
        return "Manage website: " + website.getName();
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("isRecip")) {
            // is adding/removing a group
            String groupName = parameters.get("group");
            String sIsRecip = parameters.get("isRecip");
            if ("true".equals(sIsRecip)) {
                addGroup(groupName);
            } else {
                removeGroup(groupName);
            }
            tx.commit();
            jsonResult = new JsonResult(true);
        } else if (parameters.containsKey("publicTheme")) {
            log.info("Update theme info");
            String publicTheme = parameters.get("publicTheme");
            String internalTheme = parameters.get("internalTheme");
            website.setPublicTheme(publicTheme);
            website.setInternalTheme(internalTheme);
            session.save(website);

            Repository r = website.getRepository();

            r.setAttribute("heroColour1", parameters.get("heroColour1"), session);
            r.setAttribute("heroColour2", parameters.get("heroColour2"), session);
            r.setAttribute("textColour1", parameters.get("textColour1"), session);
            r.setAttribute("textColour2", parameters.get("textColour2"), session);
            r.setAttribute("logo", parameters.get("logo"), session);
            r.setAttribute("menu", parameters.get("menu"), session);
            tx.commit();
            jsonResult = new JsonResult(true);
        } else if (parameters.containsKey("name")) {
            try {
                // the details tab
                website.setName(parameters.get("name"));
                website.setDomainName(parameters.get("domainName"));
                website.setRedirectTo(parameters.get("redirectTo"));
                System.out.println("redirect: " + website.getRedirectTo());
                Website aliasTo = null;
                String sAlias = parameters.get("aliasTo");
                if (sAlias != null && sAlias.trim().length() != 0) {
                    aliasTo = Website.findByName(sAlias.trim(), session);
                }
                website.setAliasTo(aliasTo);

                session.save(website);
                tx.commit();
                jsonResult = new JsonResult(true);
            } catch (Exception ex) {
                jsonResult = new JsonResult(false, ex.getLocalizedMessage());
                tx.rollback();
            }
        } else if (parameters.containsKey("settingsAppId")) {
            updateApplicationSettings(parameters, files, tx);
        } else if (parameters.containsKey("appId")) {
            updateApplicationEnabled(parameters, session, tx);
        }

        return null;
    }

    private void removeGroup(String groupName) {
        Group group = website.getOrganisation().group(groupName, SessionManager.session());
        website.removeGroup(group, SessionManager.session());
    }

    private void addGroup(String groupName) {
        Group group = website.getOrganisation().group(groupName, SessionManager.session());
        website.addGroup(group, SessionManager.session());
    }

    public List<String> getThemes() {
        List<String> list = new ArrayList<>();
        list.add("fuse");
        list.add("milton");
        list.add("custom");
        return list;
    }

    public Map<String, String> getThemeParams() {
        if (themeParams == null) {
            themeParams = new HashMap<>();
            if (website.getRepository().getNvPairs() != null) {
                for (NvPair pair : website.getRepository().getNvPairs()) {
                    themeParams.put(pair.getName(), pair.getPropValue());
                }
            }
        }
        return themeParams;
    }

    @Override
    public List<AppControlBean> getApps() {
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
            // push web and org into request variables for templating
            RenderAppSettingsDirective.setOrganisation(organisation);
            RenderAppSettingsDirective.setWebsite(website);

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

    public List<Website> getOtherWebsites() {
        List<Website> websites = website.getOrganisation().getWebsites();
        if (websites == null) {
            websites = Collections.EMPTY_LIST;
        }
        websites.remove(website);
        return websites;
    }

    public List<Group> getSelectedGroups() {
        List<Group> list = new ArrayList<>();
        for (GroupInWebsite giw : website.groups(SessionManager.session())) {
            list.add(giw.getUserGroup());
        }
        return list;
    }

    public List<Group> getAllGroups() {
        return getOrganisation().groups(SessionManager.session());
    }

    public boolean isSelected(Group g) {
        return getSelectedGroups().contains(g);
    }
}
