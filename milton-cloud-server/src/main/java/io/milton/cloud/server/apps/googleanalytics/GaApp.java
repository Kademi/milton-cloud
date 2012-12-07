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
package io.milton.cloud.server.apps.googleanalytics;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
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
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 * This application inserts the Google Analytics header script so that page access
 * is logged into GA
 *
 * @author brad
 */
public class GaApp implements PortletApplication, SettingsApplication{

    private AppConfig config;
    
    @Override
    public String getInstanceId() {
        return "GoogleAnalytics";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Google Analytics";
    }
    
    

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        String gaAccNum;
        if( websiteBranch != null ) {
            gaAccNum = config.get("gaAccountNumber", websiteBranch);
        } else {
            gaAccNum = config.get("gaAccountNumber", organisation);
        }
        if( gaAccNum == null ) {
            return "Inserts google analytics logging script into website pages.";
        } else {
            return "Inserts google analytics logging script into website pages. Account number: " + gaAccNum;
        }        
    }

    
    
    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.config = config;
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if( PortletApplication.PORTLET_SECTION_HEADER.equals(portletSection) ) {
            String gaAccNum = findSetting("gaAccountNumber", rootFolder);
            writeGaScript(writer, gaAccNum);
        }
    }

    private void writeGaScript(Writer writer, String gaAccNum) throws IOException {
        if( gaAccNum == null ) {
            gaAccNum = "";
        }
        writer.write("<script type=\"text/javascript\">\n");
        writer.write("var _gaq = _gaq || [];\n");
        writer.write("_gaq.push(['_setAccount', '" + gaAccNum + "']);\n");
        writer.write("_gaq.push(['_trackPageview']);\n");
        writer.write("(function() {\n");
        writer.write("var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;\n");
        writer.write("ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';\n");
        writer.write("var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);\n");
        writer.write("})();\n");
        writer.write("</script>\n");
    }


    private String findSetting(String setting, RootFolder rootFolder) {
        if( rootFolder instanceof WebsiteRootFolder ) {
            return config.get(setting, ((WebsiteRootFolder)rootFolder).getBranch());
        } else {
            return config.get(setting, rootFolder.getOrganisation());
        }
    }    
        
    
    @Override
    public void renderSettings(Profile currentUser, Organisation org, Branch websiteBranch, Context context, Writer writer) throws IOException {        
        String acc; // = findSetting("gaAccountNumber", rootFolder);
        if( websiteBranch != null ) {
            acc = config.get("gaAccountNumber", websiteBranch);
        } else {
            acc = config.get("gaAccountNumber", org);
        }
        
        if( acc == null ) {
            acc = "";
        }
        writer.write("<label for=''>Google Analytics Account Number</label>");
        writer.write("<input type='text' name='gaAccNum' value='" + acc + "' />");
        writer.flush();
    }

    @Override
    public JsonResult processForm(Map<String, String> parameters, Map<String, FileItem> files, Organisation org, Branch websiteBranch) throws BadRequestException, NotAuthorizedException, ConflictException {        
        String newAcc = parameters.get("gaAccNum");
        if( websiteBranch != null ) {
            config.set("gaAccountNumber", websiteBranch, newAcc);
        } else {
            config.set("gaAccountNumber", org, newAcc);
        }
        return new JsonResult(true);
    }
    
}
