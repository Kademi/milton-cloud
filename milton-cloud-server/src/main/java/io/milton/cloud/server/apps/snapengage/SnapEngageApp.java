/*
 * Copyright 2013 McEvoy Software Ltd.
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
package io.milton.cloud.server.apps.snapengage;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 *
 * @author brad
 */
public class SnapEngageApp implements PortletApplication, SettingsApplication {
    public static final String SNAPENGAGEID = "snapengage.id";

    private AppConfig config;
    
    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        CommonResource r = (CommonResource) context.get("page");
        switch (portletSection) {
            case PortletApplication.PORTLET_SECTION_END_OF_PAGE:
                writeSnapEngageScript(rootFolder, writer);
        }
    }

    private void writeSnapEngageScript(RootFolder rootFolder, Writer writer) throws IOException {
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            String id = findSetting(SNAPENGAGEID, rootFolder);
            writer.write("<script type='text/javascript'>\n");
            writer.write("  (function() {\n");
            writer.write("    var se = document.createElement('script'); se.type = 'text/javascript'; se.async = true;\n");
            writer.write("    se.src = '//commondatastorage.googleapis.com/code.snapengage.com/js/" + id + ".js';\n");
            writer.write("    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(se, s);\n");
            writer.write("  })();\n");
            writer.write("</script>\n");
        }
    }

    @Override
    public String getInstanceId() {
        return "SnapEngage";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.config = config;
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Snap Engage Chat";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Allows you to get feedback from users and chat to them in realtime with the SnapEngage.com service.";
    }

    @Override
    public void renderSettings(Profile currentUser, Organisation org, Branch websiteBranch, Context context, Writer writer) throws IOException {
        String acc = null; // = findSetting("gaAccountNumber", rootFolder);
        if( websiteBranch != null ) {
            acc = config.get(SNAPENGAGEID, websiteBranch);
        } 
        if( acc == null) {
            acc = config.get(SNAPENGAGEID, org);
        }        
        if( acc == null ) {
            acc = "";
        }
        writer.write("<label for=''>Snap Engage Account Number</label>");
        writer.write("<input type='text' name='" + SNAPENGAGEID + "' value='" + acc + "' />");
        writer.flush();        
    }

    @Override
    public JsonResult processForm(Map<String, String> parameters, Map<String, FileItem> files, Organisation org, Branch websiteBranch) throws BadRequestException, NotAuthorizedException, ConflictException {
        String newAcc = parameters.get(SNAPENGAGEID);
        if( websiteBranch != null ) {
            config.set(SNAPENGAGEID, websiteBranch, newAcc);
        } else {
            config.set(SNAPENGAGEID, org, newAcc);
        }
        return new JsonResult(true);
    }
    
    private String findSetting(String setting, RootFolder rootFolder) {
        String s = null;
        if( rootFolder instanceof WebsiteRootFolder ) {
            s = config.get(setting, ((WebsiteRootFolder)rootFolder).getBranch());
        }
        if( s == null ) {
            s = config.get(setting, rootFolder.getOrganisation());
        }
        return s;
    }    
            
}
