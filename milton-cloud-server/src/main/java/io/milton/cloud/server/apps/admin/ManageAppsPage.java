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
import io.milton.cloud.server.apps.website.AppsPageHelper;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.RenderAppSettingsDirective;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Controls application and their settings for an organisation. See
 * ManageWebsitePage for managing apps within a Website
 *
 * @author brad
 */
public class ManageAppsPage extends AbstactAppsPage implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageAppsPage.class);

    public ManageAppsPage(String name, Organisation organisation, CommonCollectionResource parent) {
        super(name, organisation, parent, null);
    }

    public String getTitle() {
        return "Manage applications: " + organisation.getName();
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("settingsAppId")) {
            jsonResult = _(AppsPageHelper.class).updateApplicationSettings(getOrganisation(), websiteBranch, parameters, files, tx);
        } else if (parameters.containsKey("appId")) {
            jsonResult = _(AppsPageHelper.class).updateApplicationEnabled(getOrganisation(), websiteBranch,parameters, session, tx);
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            // push web and org into request variables for templating
            RenderAppSettingsDirective.setOrganisation(organisation);
            RenderAppSettingsDirective.setWebsiteBranch(websiteBranch);

            _(HtmlTemplater.class).writePage("admin", "admin/manageApps", this, params, out);
        }
    }

    @Override
    public boolean is(String type) {
        if (type.equals("apps")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public List<AppControlBean> getApps() {
        return _(AppsPageHelper.class).getApps(getOrganisation(), websiteBranch);
    }
}
