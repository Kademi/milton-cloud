/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.orgs;

import io.milton.cloud.server.apps.user.ProfilePage;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 *
 * @author brad
 */
public class ViewOrgPage extends TemplatedHtmlPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProfilePage.class);

    private  OrganisationFolder parent;
    
    public ViewOrgPage(String name, OrganisationFolder parent) {
        super(name, parent, "admin/viewOrg", "View " + parent.getOrganisation().getOrgId() );
        this.parent = parent;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuDashboard", "menuGroupsUsers", "menuOrgs");
        super.sendContent(out, range, params, contentType);
    }

    @Override
    public String getTitle() {
        String s = "View ";
        if( parent.getOrganisation().getTitle() != null  )  {
            s += parent.getOrganisation().getTitle();
        } else {
            s += parent.getOrganisation().getOrgId();
        }
        return s;
    }


}

