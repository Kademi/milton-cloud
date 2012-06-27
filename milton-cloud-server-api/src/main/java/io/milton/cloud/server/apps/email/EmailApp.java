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
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.HtmlTemplateRenderer;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class EmailApp implements MenuApplication{

    @Override
    public String getInstanceId() {
        return "email";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof GroupEmailAdminFolder) {
            GroupEmailAdminFolder faf = (GroupEmailAdminFolder) parent;
            if (requestedName.equals("manage")) {
                MenuItem.setActiveIds("menuTalk", "menuEmails", "menuSendEmail");
                return new ManageGroupEmailsPage(requestedName, faf.getOrganisation(), faf);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder orgFolder = (OrganisationFolder) parent;
            children.add(new GroupEmailAdminFolder("groupEmails", orgFolder, orgFolder.getOrganisation()));
        }        
    }

    @Override
    public void appendMenu(MenuItem parent) {
        OrganisationFolder parentOrg = HtmlTemplateRenderer.findParentOrg(parent.getResource());
        switch (parent.getId()) {
            case "menuRoot":
                parent.getOrCreate("menuTalk", "Talk &amp; Connect").setOrdering(30);
                break;
            case "menuTalk":
                parent.getOrCreate("menuEmails", "Send emails").setOrdering(20);
                break;
            case "menuEmails":
                parent.getOrCreate("menuSendEmail", "Send and manage emails", parentOrg.getPath().child("groupEmails").child("manage")).setOrdering(10);
                break;
        }
    }
    
}
