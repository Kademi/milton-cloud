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
package io.milton.cloud.server.apps.search;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.Context;

/**
 *
 * @author brad
 */
public class SearchApp implements PortletApplication {

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        // TODO: Currently uses google, but need to provide option of google search or lucene/solr
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            if (PortletApplication.PORTLET_SECTION_NAV_HEADER.equals(portletSection)) {
                writer.write("<div class='divider'></div>\n");
                writer.write("<form action='http://www.google.com/search' method='get'>\n");
                writer.write("<fieldset>\n");
                writer.write("<input type='hidden' name='domains' value='" + wrf.getWebsite().getDomainName() +"'/>\n");
                writer.write("<input type='hidden' name='sitesearch' value='" + wrf.getWebsite().getDomainName() +"' />\n");
                writer.write("<div class='inputBox'>\n");
                writer.write("<input class='input' type='text' name='q' placeholder='Search'/>\n");
                writer.write("<button class='goBtn' name='btnG' type='submit'>Go</button> \n");
                writer.write("</div>\n");
                writer.write("</fieldset>\n");
                writer.write("<div class='clr'></div>\n");
                writer.write("</form>\n\n");
            }
        }
    }

    @Override
    public String getInstanceId() {
        return "search";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Website Search";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Allows users to search for content in a website";
    }
}
