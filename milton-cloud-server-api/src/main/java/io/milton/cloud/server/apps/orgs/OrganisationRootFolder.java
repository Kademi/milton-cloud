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
package io.milton.cloud.server.apps.orgs;

import io.milton.http.exceptions.NotAuthorizedException;
import java.util.*;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Organisation;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;

import static io.milton.context.RequestContext._;

/**
 * This is the root folder for the admin site. The admin site is used to setup
 * users and websites accessing the server
 *
 * @author brad
 */
public class OrganisationRootFolder extends OrganisationFolder implements RootFolder {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationRootFolder.class);
    private final ApplicationManager applicationManager;
    private final Organisation organisation;
    private ResourceList children;
    private Map<String,Object> attributes;

    public OrganisationRootFolder( ApplicationManager applicationManager, Organisation organisation) {
        super(null, organisation);
        this.applicationManager = applicationManager;
        this.organisation = organisation;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = applicationManager.getPage(this, childName);
        if (r != null) {
            return r;
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public PrincipalResource findEntity(BaseEntity u) throws NotAuthorizedException, BadRequestException{
        return UserApp.findEntity(u, this);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (organisation.getRepositories() != null) {
                for (Repository repo : organisation.getRepositories()) {
                    RepositoryFolder rf = new RepositoryFolder(this, repo, false);
                    children.add(rf);
                }
            }
            children.add(new OrganisationsFolder("organisations", this, organisation));
            
            _(ApplicationManager.class).addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public Map<String, Object> getAttributes() {
        if( attributes == null ) {
            attributes = new HashMap<>();
        }
        return attributes;
    }    
}
