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
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.DataResourceApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.event.WebsiteCreatedEvent;
import io.milton.cloud.server.role.Role;
import io.milton.cloud.server.web.AbstractContentResource;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonRepositoryResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.Pair;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import org.apache.velocity.context.Context;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.utils.SessionManager;

/**
 * Does lots of things to do with content delivery.
 *
 * The main thing it does is convert FileResource objects to RenderFileResource
 * objects, for html files which are accessed within a website. This is what
 * allows templating to occur.
 *
 * Note that this will not return a RenderFileResource for html files which have
 * a doctype
 *
 * @author brad
 */
public class ContentApp implements Application, PortletApplication, ResourceApplication, MenuApplication, DataResourceApplication, ChildPageApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContentApp.class);
    public static final String ROLE_CONTENT_VIEWER = "Content Viewer";
    public static final String PATH_SUFFIX_HISTORY = ".history";
    public static final String PATH_SUFFIX_PREVIEW = ".preview";
    private SpliffyResourceFactory resourceFactory;

    @Override
    public String getInstanceId() {
        return "content";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Content viewing and authoring";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides content viewing functions, such as generating menus based on folders";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.resourceFactory = resourceFactory;
        resourceFactory.getSecurityManager().add(new ContentViewerRole());
        resourceFactory.getSecurityManager().add(new ContentAuthorRole());
        resourceFactory.getEventManager().registerEventListener(new EventListener() {
            @Override
            public void onEvent(Event e) {
                if( e instanceof WebsiteCreatedEvent ) {
                    WebsiteCreatedEvent wce = (WebsiteCreatedEvent) e;
                    createDefaultWebsiteContent(wce.getWebsite());
                }
            }
        }, WebsiteCreatedEvent.class);
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (rootFolder instanceof WebsiteRootFolder) {
            if (portletSection.equals("endOfPage")) {
                // Inject resources to permit inline editing
                if (currentUser != null) { // but don't bother if no one logged in
                    writer.append("<script type='text/javascript' src='/static/inline-edit/inline-edit.js'>//</script>\n");
                    writer.append("<link href='/static/inline-edit/inline-edit.css' rel='stylesheet' type='text/css' />\n");
                }
                // Resources for classifier - TODO: make this configurable somehow
                writer.append("<script type='text/javascript' src='/static/common/jquery.debounce-1.0.5.js'>//</script>\n");
            }
        }
    }

    @Override
    public Resource getResource(RootFolder webRoot, String path) throws NotAuthorizedException, BadRequestException {
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
        appendWebsiteMenu(parent);
    }

    /**
     * Get the current website and look for a "menu" attribute on its
     * repository. If it exists then parse it and generate menu items
     *
     * @param parent
     */
    public void appendWebsiteMenu(MenuItem parent) {
        String thisHref = null;
        Request req = HttpManager.request();
        if (req != null) {
            thisHref = req.getAbsolutePath();
        }
        if (parent.getId().equals("menuRoot")) {
            RootFolder rootFolder = parent.getRootFolder();
            List<Pair<String, String>> pairs = WebUtils.getThemeMenu(rootFolder);
            if (pairs != null) {
                int cnt = 0;
                for (Pair<String, String> pair : pairs) {
                    String id = "menuContent" + cnt++;
                    String menuHref = pair.getObject1();
                    MenuItem i = parent.getOrCreate(id, pair.getObject2(), menuHref);
                    i.setOrdering(cnt * 10);
                    if (thisHref != null && thisHref.startsWith(menuHref)) {
                        MenuItem.setActiveId(id);
                    }
                }
            }
        }
    }

    @Override
    public ContentResource instantiateResource(Object sourceObject, CommonCollectionResource parent, RootFolder rf) {
        if (rf instanceof WebsiteRootFolder && sourceObject instanceof DataSession.FileNode) {
            FileNode fn = (FileNode) sourceObject;
            if (fn.getName().endsWith(".html")) {
                if (parent instanceof ContentDirectoryResource) {
                    ContentDirectoryResource contentParent = (ContentDirectoryResource) parent;

                    // Only wrap if content is directly within a website. Resource which are included
                    // from sibling repositories should be presented as-is, ie without rendering, because
                    // they tend to be used for static resources which can include html pages
                    CommonResource closestBranch = contentParent.closest("branch");
                    if (closestBranch == rf) {
                        FileResource fr = new FileResource(fn, contentParent);
                        // Dont use a render resource for files with a doctype. This provides a way to have plain html files which dont get parsed or rendered
                        RenderFileResource rfr = fr.getHtml();
                        return rfr;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof BranchFolder && requestedName.equals("commits")) {
            BranchFolder branchRes = (BranchFolder) parent;
            return new CommitsResource(requestedName, branchRes);
        }
        return null;
    }

    public class ContentViewerRole implements Role {

        @Override
        public String getName() {
            return ROLE_CONTENT_VIEWER;
        }

        @Override
        public boolean appliesTo(CommonResource resource, Organisation withinOrg, Group g) {
            if (isContentResource(resource)) {
                //log.info("appliesTo(org): resource=" + resource);
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

        @Override
        public boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g) {
            if (resource instanceof CommonRepositoryResource) {                
                CommonRepositoryResource cr = (CommonRepositoryResource) resource;
                boolean  b = (cr.getRepository() == applicableRepo);
                //log.info("appliesTo(repo): resource=" + resource + " = " + b);
                //log.info("cr.repo=" + cr.getRepository().getName() + " applica=" + applicableRepo.getName());
                return b;
            } else {
                return false;
            }
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g) {
            return Collections.singleton(AccessControlledResource.Priviledge.READ);
        }
    }

//    TODO: These roles should return privs as configured for the repository
//            this will allow in milton content viewers to edit content for website repo
//    
//                    
//                    Also, add Repository access screen to website
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

        @Override
        public boolean appliesTo(CommonResource resource, Repository applicableRepo, Group g) {
            if (resource instanceof CommonRepositoryResource) {
                CommonRepositoryResource cr = (CommonRepositoryResource) resource;
                return (cr.getRepository() == applicableRepo);
            } else {
                return false;
            }
        }

        @Override
        public Set<Priviledge> getPriviledges(CommonResource resource, Repository applicableRepo, Group g) {
            return Role.READ_WRITE;
        }
    }

    private void createDefaultWebsiteContent(Website website) {
        Profile user = _(SpliffySecurityManager.class).getCurrentUser();
        Branch b = website.liveBranch();
        createDefaultWebsiteContent(b, user, SessionManager.session());
    }
    
    private void createDefaultWebsiteContent(Branch websiteBranch, Profile user, Session session) {
        try {
            String html = "";
            html += "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
            html += "<head>\n";
            html += "<title>new page1</title>\n";
            html += "<link rel=\"template\" href=\"theme/page\" />\n";
            html += "</head>\n";
            html += "<body>\n";
            html += "<h1>Welcome to your new site!</h1>\n";
            html += "<p>Login with the menu in the navigation above, then you will be able to start editing</p>\n";
            html += "</body>\n";
            html += "</html>\n";
            DataSession dataSession = new DataSession(websiteBranch, session, _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
            DataSession.DirectoryNode rootDir = (DataSession.DirectoryNode) dataSession.find(Path.root);
            FileNode file = rootDir.addFile("index.html");
            ByteArrayInputStream bin = new ByteArrayInputStream(html.getBytes("UTF-8"));
            file.setContent(bin);
            dataSession.save(user);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
