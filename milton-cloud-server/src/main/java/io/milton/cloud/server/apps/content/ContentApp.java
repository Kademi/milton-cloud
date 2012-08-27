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
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.AbstractContentResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import org.apache.velocity.context.Context;

/**
 *
 * @author brad
 */
public class ContentApp implements Application, PortletApplication, ResourceApplication, MenuApplication {
    
    public static final String ROLE_CONTENT_VIEWER = "Content Viewer";
    public static final String PATH_SUFFIX_HISTORY = ".history";
    public static final String PATH_SUFFIX_PREVIEW = ".preview";
    private SpliffyResourceFactory resourceFactory;
    
    @Override
    public String getInstanceId() {
        return "content";
    }
    
    @Override
    public String getTitle(Organisation organisation, Website website) {
        return "Content viewing and authoring";
    }
    
    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Provides content viewing functions, such as generating menus based on folders";
    }
    
    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.resourceFactory = resourceFactory;
        resourceFactory.getSecurityManager().add(new ContentViewerRole());
        resourceFactory.getSecurityManager().add(new ContentAuthorRole());
    }
    
    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (rootFolder instanceof WebsiteRootFolder) {
            if (portletSection.equals("header")) {
                // Inject resources to permit inline editing
                if (currentUser != null) { // but don't bother if no one logged in
                    writer.append("<script type='text/javascript' src='/static/inline-edit/inline-edit.js'>//</script>\n");
                    writer.append("<link href='/static/inline-edit/inline-edit.css' rel='stylesheet' type='text/css' />\n");
                }
                // Resources for classifier - TODO: make this configurable somehow
                writer.append("<script type='text/javascript' src='/static/classifier/jquery.mc-classifier.js'>//</script>\n");
                writer.append("<script type='text/javascript' src='/static/common/jquery.debounce-1.0.5.js'>//</script>\n");
                writer.append("<link href='/static/classifier/classifier.css' rel='stylesheet' type='text/css' />\n");
            }
        }
    }
    
    @Override
    public Resource getResource(RootFolder webRoot, String path) throws NotAuthorizedException, BadRequestException {
        System.out.println("getResource: " + path);
        if (path.endsWith(PATH_SUFFIX_HISTORY)) {
            String resourcePath = path.substring(0, path.length() - PATH_SUFFIX_HISTORY.length()); // chop off suffix to get real resource path
            Path p = Path.path(resourcePath);
            Resource r = findFromRoot(webRoot, p);
            if (r != null) {
                if (r instanceof ContentResource) {
                    ContentResource cr = (ContentResource) r;
                    return new HistoryResource(p.getName(), cr);
                }
            }
        } else if (path.endsWith(PATH_SUFFIX_PREVIEW)) {
            String resourcePath = path.substring(0, path.length() - PATH_SUFFIX_PREVIEW.length()); // chop off suffix to get real resource path
            Path p = Path.path(resourcePath);
            Resource r = findFromRoot(webRoot, p);
            if (r != null) {
                if (r instanceof ContentResource) {
                    ContentResource cr = (ContentResource) r;
                    return new ViewFromHistoryResource(p.getName(), cr);
                }
            }
            
        }
        return null;
    }
    
    private Resource findFromRoot(RootFolder rootFolder, Path p) throws NotAuthorizedException, BadRequestException {
        CollectionResource col = rootFolder;
        Resource r = null;
        for (String s : p.getParts()) {
            if (col == null) {
                return null;
            }
            r = col.child(s);
            if (r == null) {
                return null;
            }
            if (r instanceof CollectionResource) {
                col = (CollectionResource) r;
            } else {
                col = null;
            }
        }
        return r;
    }
    
    @Override
    public void appendMenu(MenuItem parent) {
        WebUtils.appendMenu(parent);
    }
   
 
    
    public class ContentViewerRole implements Role {
        
        @Override
        public String getName() {
            return ROLE_CONTENT_VIEWER;
        }
        
        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if (isContentResource(resource)) {
                ContentResource cr = (ContentResource) resource;
                return cr.getOrganisation().isWithin(withinOrg);
            }
            return false;
        }
        
        @Override
        public Set<AccessControlledResource.Priviledge> getPriviledges(CommonResource resource, Organisation withinOrg, Group g) {
            return Collections.singleton(AccessControlledResource.Priviledge.READ);
        }
        
        private boolean isContentResource(CommonResource resource) {
            return resource instanceof ContentResource;
        }
    }
    
    public class ContentAuthorRole implements Role {
        
        @Override
        public String getName() {
            return "Content author";
        }
        
        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if (resource instanceof AbstractContentResource) {
                AbstractContentResource acr = (AbstractContentResource) resource;
                return acr.getOrganisation().isWithin(withinOrg);
            }
            if (resource instanceof RenderFileResource) {
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
