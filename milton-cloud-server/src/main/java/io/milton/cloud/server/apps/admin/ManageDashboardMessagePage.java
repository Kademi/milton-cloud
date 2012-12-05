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

import edu.emory.mathcs.backport.java.util.Arrays;
import io.milton.cloud.server.apps.user.DashboardMessageUtils;
import io.milton.cloud.server.apps.website.ManageWebsiteBranchFolder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Website;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageDashboardMessagePage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageDashboardMessagePage.class);
    private final ManageWebsiteBranchFolder parent;
    private final Group group;
    private JsonResult jsonResult;
    private String _html;
    private String _position;

    public ManageDashboardMessagePage(Group group, ManageWebsiteBranchFolder parent) {
        this.group = group;
        this.parent = parent;
    }

    public String getTitle() {
        return "Dashboard message for " + group.getName() + " in website " + parent.getWebsite().getName();
    }
    
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (parameters.containsKey("html")) {
            String html = WebUtils.getParam(parameters, "html");
            String position = WebUtils.getParam(parameters, "position");
            try {
                Properties props = DashboardMessageUtils.messageProps(HttpManager.request(), group, parent.getBranch());
                if( html != null ) {
                    props.setProperty("html", html);
                } else {
                    props.remove("html");
                }
                if( position != null ) {
                props.setProperty("position", position);
                } else {
                    props.remove("position");
                }
                DashboardMessageUtils.saveProps(props, HttpManager.request(), group, parent.getBranch());
                DashboardMessageUtils.dataSession(HttpManager.request(), group, parent.getBranch()).save(_(SpliffySecurityManager.class).getCurrentUser());
                tx.commit();
                jsonResult = new JsonResult(true);
            } catch (IOException ex) {
                log.error("exception setting content", ex);
                jsonResult = new JsonResult(false, "Exception occured: " + ex.getMessage());
            }

        }
        return null;
    }

    public List<String> getAllPositions() {
        List<String> list = new ArrayList<>();
        list.add("top");
        list.add("bottom");
        return list;
    }
    
    /**
     * Get the HTML message
     *
     * @return
     */
    public String getHtml() throws IOException {
        if (_html == null) {
            Properties props = DashboardMessageUtils.messageProps(HttpManager.request(), group, parent.getBranch());
            if (props.containsKey("html")) {
                _html = props.getProperty("html");
            } else {
                _html = "";
            }
        }
        return _html;
    }
    
    public String getPosition() throws IOException {
        if( _position == null ) {
            Properties props = DashboardMessageUtils.messageProps(HttpManager.request(), group, parent.getBranch());
            if (props.containsKey("position")) {
                _position = props.getProperty("position");
            } else {
                _position = "";
            }            
        }
        return _position;
    }
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuDashboardMessages", "menuDashboardMessages");
            _(HtmlTemplater.class).writePage("admin", "dashboard/manageDashboardMessages", this, params, out);
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
        return "_dashboard_" + group.getName();
    }

    public Group getGroup() {
        return group;
    }

    public Website getWebsite() {
        return parent.getWebsite();
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
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
