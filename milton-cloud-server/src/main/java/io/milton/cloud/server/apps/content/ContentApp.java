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
package io.milton.cloud.server.apps.content;

import edu.emory.mathcs.backport.java.util.Collections;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.admin.AdminApp;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.AbstractContentResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import java.util.Set;

/**
 *
 * @author brad
 */
public class ContentApp implements Application {

    public static final String ROLE_CONTENT_VIEWER = "Content Viewer";
    
    @Override
    public String getInstanceId() {
        return "content";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        resourceFactory.getSecurityManager().add(new ContentViewerRole());
        resourceFactory.getSecurityManager().add(new ContentAuthorRole());
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        
    }

    public class ContentViewerRole implements Role {

        @Override
        public String getName() {
            return ROLE_CONTENT_VIEWER;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if( isContentResource(resource) ) {
                AbstractContentResource acr = (AbstractContentResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }            
            return false;
        }

        @Override
        public Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Collections.singleton(AccessControlledResource.Priviledge.READ);
        }

        private boolean isContentResource(CommonResource resource) {
            return resource instanceof RenderFileResource || resource instanceof AbstractContentResource;
        }
    }
    
    public class ContentAuthorRole implements Role {

        @Override
        public String getName() {
            return "Content author";
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if( resource instanceof AbstractContentResource ) {
                AbstractContentResource acr = (AbstractContentResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }
            if( resource instanceof RenderFileResource ) {
                RenderFileResource acr = (RenderFileResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }
            return false;
        }

        @Override
        public Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Role.READ_WRITE;
        }
    }        
}
