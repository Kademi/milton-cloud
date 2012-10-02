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
package io.milton.cloud.server.apps.website;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
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
import io.milton.resource.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageWebsitesFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsitesFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation organisation;
    private ResourceList children;
    private JsonResult jsonResult;

    public ManageWebsitesFolder(String name, Organisation organisation, CommonCollectionResource parent) {
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }

    public String getTitle() {
        return "Manage websites";
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        String newName = WebUtils.getParam(parameters, "newName");
        if (newName != null) {
            newName = newName.trim().toLowerCase();
            log.info("processForm: newName: " + newName);
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            String newDnsName = WebUtils.getParam(parameters, "newDnsName");
            if( newDnsName != null ) {
                newDnsName = newDnsName.trim().toLowerCase();
            }

            {
                Website existing = Website.findByName(newName, session);
                if (existing != null) {
                    jsonResult = new JsonResult(false, "Domain name is already registered in this system");
                    jsonResult.addFieldMessage("newName", "Please choose a unique name");
                    return null;
                }
            }
            Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
            Website website = getOrganisation().createWebsite(newName, newDnsName, null, curUser, session);
            Branch websiteBranch = website.liveBranch();
            Repository r = website;
            r.setPublicContent(true); // allow public access
            session.save(r);
            
            Map<String,String> themeParams = new HashMap<>();
            themeParams.put("heroColour1", "#88c03f");
            themeParams.put("heroColour2", "#88c03f");
            themeParams.put("textColour1", "#1C1D1F");
            themeParams.put("textColour2", "#2F2F2F");
            
            Map<String,String> themeAttributes = new HashMap<>();
            themeAttributes.put("logo", "<img src='/content/theme/images/IDH_logo.png' >");            
            
            ManageWebsiteBranchFolder websiteBranchFolder = new ManageWebsiteBranchFolder(website, websiteBranch, this);
            try {
                websiteBranchFolder.setThemeParams(themeParams);
                websiteBranchFolder.setThemeAttributes(themeAttributes);
                websiteBranchFolder.save();
            } catch (IOException ex) {
                jsonResult = new JsonResult(false, ex.getMessage());
                return null;
            }
            
            Date now = _(CurrentDateService.class).getNow();
            AppControl.initDefaultApps(websiteBranch, curUser, now, session);
            createDefaultWebsiteContent(websiteBranch, curUser, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Created", website.getDomainName());
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuDashboard", "menuWebsiteManager", "menuWebsites");
            _(HtmlTemplater.class).writePage("admin", "admin/manageWebsites", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for (Website w : getWebsites()) {
                ManageWebsiteFolder p = new ManageWebsiteFolder(this, w);
                children.add(p);
            }
        }
        return children;
    }
    
    public List<ManageWebsiteFolder> getWebsiteFolders()  throws NotAuthorizedException, BadRequestException {
        List<ManageWebsiteFolder> list = new ArrayList<>();
        for( Resource r : getChildren() ) {
            if( r instanceof ManageWebsiteFolder) {
                list.add((ManageWebsiteFolder)r);
            }
        }
        return list;
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
        return organisation;
    }

    public List<Website> getWebsites() {
        return organisation.websites();
    }

    public String websiteAddress(Website w) {
        return w.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    private void createDefaultWebsiteContent(Branch websiteBranch, Profile user, Session session) {
        try {
            String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
            html += "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
            html += "<head>\n";
            html += "<title>new page1</title>\n";
            html += "<link rel=\"template\" href=\"theme/page\" />\n";
            html += "</head>\n";
            html += "<body>\n";
            html += "<h1>Welcome to your new site!</h1>\n";
            html += "<p>Login with the menu in the navigation above, then you will be able to start editing</p>\n";
            html += "</body>\n";
            html += "</html>\n";            
            DataSession dataSession = new DataSession(websiteBranch, session, _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
            DataSession.DirectoryNode rootDir = (DataSession.DirectoryNode) dataSession.find(Path.root);
            FileNode file = rootDir.addFile("index.html");
            ByteArrayInputStream bin = new ByteArrayInputStream(html.getBytes("UTF-8"));
            file.setContent(bin);
            dataSession.save(user);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
