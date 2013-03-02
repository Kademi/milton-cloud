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
package io.milton.cloud.server.apps.colorpicker;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.Context;

/**
 *
 * @author brad
 */
public class ColorPickerApp implements PortletApplication {

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if( PortletApplication.PORTLET_SECTION_END_OF_PAGE.equals(portletSection) ) {
            if (currentUser != null) { // don't bother if no one logged in
                writer.append("<script type='text/javascript' src='/templates/apps/colorpicker/jquery-miniColors/jquery.miniColors.js'>//</script>\n");
                writer.append("<script type='text/javascript' src='/templates/apps/colorpicker/init-colorpicker.js'>//</script>\n");
                writer.append("<link href='/templates/apps/colorpicker/jquery-miniColors/jquery.miniColors.css' rel='stylesheet' type='text/css' />\n");
                writer.append("<link href='/templates/apps/colorpicker/colorpicker-layout.css' rel='stylesheet' type='text/css' />\n");
            }
        }
    }

    @Override
    public String getInstanceId() {
        return "colorPicker";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Color Picker (content editing)";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides editor support for choosing color values from a graphical colour pallette, by injecting javascript references";
    }
}
