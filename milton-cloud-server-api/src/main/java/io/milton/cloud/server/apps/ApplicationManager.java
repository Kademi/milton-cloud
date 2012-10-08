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
package io.milton.cloud.server.apps;

import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.mail.EmailTriggerType;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.manager.MCRootContext;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.DirectoryResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.GroupResource;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.RepositoryFolder;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.MenuItem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.mail.MessageFolder;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author brad
 */
public class ApplicationManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ApplicationManager.class);
    /**
     * Name of a system property used to specify extra apps to load. The
     * property should be comma seperated list of class names
     */
    public static final String EXTRA_APPS_SYS_PROP_NAME = "extra.apps";
    private final List<Application> apps;
    private final CurrentRootFolderService currentRootFolderService;
    private File appsConfigDir;
    private MCRootContext rootContext;
    private List<EmailTriggerType> emailTriggerTypes = new ArrayList<>();

    public ApplicationManager(List<Application> initialApps, CurrentRootFolderService currentRootFolderService) {
        this.currentRootFolderService = currentRootFolderService;
        List<Application> list = new ArrayList<>(initialApps);
        this.apps = list;
        String extraApps = System.getProperty(EXTRA_APPS_SYS_PROP_NAME);
        if (extraApps != null && !extraApps.isEmpty()) {
            log.info("Load apps from system property: " + extraApps);
            String[] arr = extraApps.split(",");
            for (String s : arr) {
                s = s.trim();
                if (s.length() > 0) {
                    Application app = initApp(s);
                    if (app != null) {
                        this.apps.add(app);
                    }
                }
            }
        }
    }

    public List<Application> getApps() {
        return apps;
    }

    public List<Application> getActiveApps() {
        RootFolder rootFolder = currentRootFolderService.getRootFolder();
        return getActiveApps(rootFolder);
    }

    public List<Application> getActiveApps(RootFolder rootFolder) {
        if (rootFolder == null) {
            log.warn("No root folder, all apps are active");
            return apps;
        } else {
            String attName = "activeApps_" + rootFolder.getId();
            List<Application> active = (List<Application>) rootFolder.getAttributes().get(attName);
            if (active == null) {
                active = findActiveApps(rootFolder);
                //log.info("init active apps for: " + rootFolder.getClass() + " = " + active.size());
                rootFolder.getAttributes().put(attName, active);
            }
            return active;
        }
    }

    public void init(SpliffyResourceFactory resourceFactory) {
        if (appsConfigDir == null) {
            throw new RuntimeException("Please configure an apps config directory in property: appsConfigDir on bean: " + this.getClass());
        }
        if (!appsConfigDir.exists()) {
            if (!appsConfigDir.mkdirs()) {
                throw new RuntimeException("Apps config folder does not exist and could not be created: " + appsConfigDir.getAbsolutePath());
            }
        }
        log.info("using apps config directory: " + appsConfigDir.getAbsolutePath());
        for (Application app : apps) {
            try {
                AppConfig config = getAppConfig(app);
                log.info("init app: " + app.getClass().getCanonicalName() + " - " + app.getInstanceId());
                app.init(resourceFactory, config);
            } catch (Exception ex) {
                log.error("Application: " + app.getInstanceId() + " failed to start", ex);
            }
        }
        System.out.println("Listing applications:");
        for (Application app : apps) {
            System.out.println(" - " + app.getInstanceId() + " : " + app.getClass());
        }
    }

    public Application get(String name) {
        for (Application app : apps) {
            if (app.getInstanceId().equals(name)) {
                return app;
            }
        }
        return null;
    }

    public void appendMenu(MenuItem parent) {
        for (Application app : getActiveApps()) {
            if (app instanceof MenuApplication) {
                ((MenuApplication) app).appendMenu(parent);
            }
        }
    }

    public MenuItem getRootMenuItem(Resource r, Profile user, RootFolder rootFolder) {
        return new MenuItem("menuRoot", this, r, user, rootFolder);
    }

    public void shutDown() {
        for (Application app : apps) {
            if (app instanceof LifecycleApplication) {
                ((LifecycleApplication) app).shutDown();
            }
        }
    }

    public Resource getPage(Resource parent, String name) {
        for (Application app : getActiveApps()) {
            if (app instanceof ChildPageApplication) {
                ChildPageApplication cpa = (ChildPageApplication) app;
                Resource child = cpa.getPage(parent, name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        for (Application app : getActiveApps()) {
            if (app instanceof BrowsableApplication) {
                BrowsableApplication ba = (BrowsableApplication) app;
                ba.addBrowseablePages(parent, children);
            }
        }
    }

    public File getAppsConfigDir() {
        return appsConfigDir;
    }

    public void setAppsConfigDir(File appsConfigDir) {
        this.appsConfigDir = appsConfigDir;
    }

    /**
     * TODO: make this read from the database
     *
     * @param app
     * @return
     */
    public AppConfig getAppConfig(Application app) throws IOException {
        File configFile = new File(appsConfigDir, app.getInstanceId() + ".properties");
        Properties props = new Properties();
        if (configFile.exists()) {
            try (InputStream fin = new FileInputStream(configFile)) {
                props.load(fin);
            }
            return new AppConfig(app.getInstanceId(), props, rootContext);
        } else {
            AppConfig config = new AppConfig(app.getInstanceId(), props, rootContext);
            if (app instanceof LifecycleApplication) {
                ((LifecycleApplication) app).initDefaultProperties(config);
                try (FileOutputStream fout = new FileOutputStream(configFile)) {
                    props.store(fout, "auto-generated defaults");
                }
                log.info("Wrote initial properties to: " + configFile.getAbsolutePath());
            }
            return config;
        }
    }

    private Application initApp(String s) {
        try {
            Class c = Class.forName(s);
            Application app = (Application) c.newInstance();
            log.info("Instantiated app: " + app.getClass().getCanonicalName());
            return app;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(s, ex);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(s, ex);
        }
    }

    /**
     * Allow applications to append priviledges to the ACL for a resource
     *
     * @param perms
     * @param user
     * @param aThis
     */
    public void appendPriviledges(List<AccessControlledResource.Priviledge> privs, Profile user, Resource r) {
        for (Application app : getActiveApps()) {
            if (app instanceof PriviledgeApplication) {
                ((PriviledgeApplication) app).appendPriviledges(privs, user, r);
            }
        }
    }

    private List<Application> findActiveApps(RootFolder rootFolder) {
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            return findActiveApps(wrf.getBranch());
        } else {
            Organisation org = rootFolder.getOrganisation();
            return findActiveApps(org);
        }
    }

    public List<Application> findActiveApps(io.milton.vfs.db.Branch websiteBranch) {
        Organisation org = (Organisation) websiteBranch.getRepository().getBaseEntity();
        List<Application> available = findActiveApps(org);
        List<AppControl> appControls = AppControl.find(websiteBranch, SessionManager.session());
        return findActiveApps(available, appControls);
    }

    /**
     * Active apps are those which are active for the parent organisation, and
     * have an AppControl record which has enabled = true
     *
     * @param org
     * @return
     */
    public List<Application> findActiveApps(Organisation org) {
        List<Application> available = findAvailableApps(org);
        List<AppControl> appControls = AppControl.find(org, SessionManager.session());
        return findActiveApps(available, appControls);
    }

    public List<Application> findAvailableApps(Organisation org) {
        if (org.getOrganisation() == null) {
            return getApps();
        } else {
            return findActiveApps(org.getOrganisation());
        }        
    }
    
    public List<Application> findActiveApps(List<Application> available, List<AppControl> appControls) {
        Map<String, Boolean> enablement = toEnabledMap(appControls);
        List<Application> activApps = new ArrayList<>();
        for (Application app : available) {
            Boolean enabled = enablement.get(app.getInstanceId());
            if (enabled != null && enabled.booleanValue()) {
                if (app != null) {
                    activApps.add(app);
                }
            }
        }
        return activApps;
    }

    private Map<String, Boolean> toEnabledMap(List<AppControl> appControls) {
        Map<String, Boolean> map = new HashMap<>();
        for (AppControl ac : appControls) {
            map.put(ac.getName(), ac.isEnabled());
        }
        return map;

    }

    public boolean isActive(Application aThis, Branch websiteBranch) {
        List<Application> activeApps = findActiveApps(websiteBranch);
        for (Application a : activeApps) {
            if (a.getInstanceId().equals(aThis.getInstanceId())) {
                return true;
            }
        }
        return false;
    }

    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, org.apache.velocity.context.Context context, Writer writer) throws IOException {
        for (Application app : getActiveApps(rootFolder)) {
            if (app instanceof PortletApplication) {
                ((PortletApplication) app).renderPortlets(portletSection, currentUser, rootFolder, context, writer);
            }
        }
    }

    public MCRootContext getRootContext() {
        return rootContext;
    }

    public void setRootContext(MCRootContext rootContext) {
        this.rootContext = rootContext;
    }

    public void storeMail(PrincipalResource principal, MimeMessage mm) {
        for (Application app : getActiveApps()) {
            if (app instanceof EmailApplication) {
                ((EmailApplication) app).storeMail(principal, mm);
            }
        }

    }

    public MessageFolder getInbox(GroupResource principal) {
        for (Application app : getActiveApps()) {
            if (app instanceof EmailApplication) {
                MessageFolder f = ((EmailApplication) app).getInbox(principal);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     *
     * Applications can register email trigger types by adding to this list.
     *
     * This will then be used in configuring triggers by other applications
     *
     * @return
     */
    public List<EmailTriggerType> getEmailTriggerTypes() {
        return emailTriggerTypes;
    }

    public ResourceList toResources(RepositoryFolder parent) {
        ResourceList children = new ResourceList();
        RootFolder rf = currentRootFolderService.peekRootFolder();
        List<DataResourceApplication> resourceCreators = getResourceCreators(rf);
        Repository repo = parent.getRepository();
        for (Branch b : repo.getBranches()) {
            CommonResource r = toResource(parent, b, resourceCreators, rf);
            System.out.println(" repo child - " + r.getName() + " - " + r.getClass());
            children.add(r);
        }
        return children;
    }
    
    private CommonResource toResource(RepositoryFolder parent, Branch branch, List<DataResourceApplication> resourceCreators, RootFolder rf) {
        for (DataResourceApplication rc : resourceCreators) {
            ContentResource r = rc.instantiateResource(branch, parent, rf);
            if (r != null) {
                return r;
            }
        }

        return new BranchFolder(branch.getName(), parent, branch);
    }

    public ResourceList toResources(ContentDirectoryResource parent, DataSession.DirectoryNode dir) {
        RootFolder rf = currentRootFolderService.peekRootFolder();
        ResourceList list = new ResourceList();
        if (dir != null) {
            List<DataResourceApplication> resourceCreators = getResourceCreators(rf);
            for (DataSession.DataNode n : dir) {
                CommonResource r = toResource(parent, n, resourceCreators, rf);
                list.add(r);
            }
        }
        return list;
    }

    /**
     * Produce a web resource representation of the given ItemHistory.
     *
     * This will be either a FileResource or a DirectoryResource, depending on
     * the type associated with the member
     *
     * @param parent
     * @param dm
     * @return
     */
    private CommonResource toResource(ContentDirectoryResource parent, DataSession.DataNode contentNode, List<DataResourceApplication> resourceCreators, RootFolder rf) {
        for (DataResourceApplication rc : resourceCreators) {
            ContentResource r = rc.instantiateResource(contentNode, parent, rf);
            if (r != null) {
                return r;
            }
        }

        if (contentNode instanceof DataSession.DirectoryNode) {
            DataSession.DirectoryNode dm = (DataSession.DirectoryNode) contentNode;
            DirectoryResource rdr = newDirectoryResource(dm, parent);
            return rdr;
        } else if (contentNode instanceof DataSession.FileNode) {
            DataSession.FileNode dm = (DataSession.FileNode) contentNode;
            FileResource fr = newFileResource(dm, parent);
            return fr;
        } else {
            throw new RuntimeException("Unknown resource type: " + contentNode);
        }
    }

    public FileResource newFileResource(DataSession.FileNode dm, ContentDirectoryResource parent) {
        return new FileResource(dm, parent);
    }

    public DirectoryResource newDirectoryResource(DataSession.DirectoryNode dm, ContentDirectoryResource parent) {
        return new DirectoryResource(dm, parent);
    }

    private List<DataResourceApplication> getResourceCreators(RootFolder rf) {
        List<DataResourceApplication> list = (List<DataResourceApplication>) rf.getAttributes().get("resourceCreators");
        if (list == null) {
            list = new ArrayList<>();
            for (Application app : getActiveApps(rf)) {
                if (app instanceof DataResourceApplication) {
                    DataResourceApplication rc = (DataResourceApplication) app;
                    list.add(rc);
                }
            }
            rf.getAttributes().put("resourceCreators", list);
        }
        return list;
    }
}
