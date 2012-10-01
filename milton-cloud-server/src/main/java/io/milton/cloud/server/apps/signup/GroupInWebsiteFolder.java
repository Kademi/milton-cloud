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
package io.milton.cloud.server.apps.signup;


import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.*;

/**
 *
 * @author brad
 */
public class GroupInWebsiteFolder extends AbstractCollectionResource{

    private final GroupInWebsite giw;
    private final WebsiteRootFolder websiteRootFolder;
    
    private ResourceList children;

    public GroupInWebsiteFolder(GroupInWebsite giw, WebsiteRootFolder websiteRootFolder) {
        this.giw = giw;
        this.websiteRootFolder = websiteRootFolder;
    }

    @Override
    public Organisation getOrganisation() {
        return websiteRootFolder.getOrganisation();
    }
    
    
     
    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
            
            // to support per group resources, we look for a directory in the main website repo
            // like /resources/[group-name]
            // if it exists we include all its children under this folder
            Resource rResources = websiteRootFolder.child("resources");
            if( rResources instanceof DirectoryResource) {
                DirectoryResource dr = (DirectoryResource) rResources;
                Resource rGroup = dr.child(giw.getUserGroup().getName());
                if( rGroup instanceof DirectoryResource) {
                    DirectoryResource dGroup = (DirectoryResource) rGroup;
                    children.addAll(dGroup.getChildren());
                }
            }
        }
        return children;
    }    
    
    @Override
    public CommonCollectionResource getParent() {
        return websiteRootFolder;
    }

    @Override
    public String getName() {
        return giw.getUserGroup().getName();
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }

    public Group getGroup() {
        return giw.getUserGroup();
    }
    
    public Website getWebsite() {
        return giw.getWebsite();
    }
}
