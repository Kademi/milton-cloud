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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.WebUtils;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import java.util.ArrayList;

/**
 * Dashboard for the user. Most functions will be provided by
 * PortletApplications which will be invoked from the template using the
 * PortletsDirective
 *
 * Eg: #portlets("messages") , which will render portlets for the messages
 * section of the page
 *
 * Standard sections are intended to be: messages - brief list of messages at
 * top of page primary - this is the main section of the page, with about 70%
 * width secondary - this is a narrowed section of the page, possible lower down
 * for small screen clients
 *
 *
 * @author brad
 */
public class DashboardPage extends TemplatedHtmlPage {

    private List<String> topMessages = new ArrayList<>();
    private List<String> bottomMessages = new ArrayList<>();

    public DashboardPage(String name, CommonCollectionResource parent, boolean requireLogin) {
        super(name, parent, "user/dashboard", "Dashboard");
        setForceLogin(requireLogin);
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        loadDashboardMessages();
        super.sendContent(out, range, params, contentType);
    }

    @Override
    public boolean is(String type) {
        if (type.equals("dashboard")) {
            return true;
        }
        return super.is(type);
    }

    private void loadDashboardMessages() throws IOException {
        WebsiteRootFolder wrf = (WebsiteRootFolder) WebUtils.findRootFolder(this);
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();

        if (p != null) {
            for (Group g : _(SpliffySecurityManager.class).getGroups(p, wrf.getWebsite())) {
                Properties props = DashboardMessageUtils.messageProps(HttpManager.request(), g, wrf.getBranch());
                String html = props.getProperty("html");
                String position = props.getProperty("position");
                if (position != null && html != null && html.length() > 0) {
                    if (position.equals("bottom")) {
                        bottomMessages.add(html);
                    } else {
                        topMessages.add(html);
                    }
                }
            }
        }
    }

    public List<String> getTopMessages() {
        return topMessages;
    }

    public List<String> getBottomMessages() {
        return bottomMessages;
    }
}
