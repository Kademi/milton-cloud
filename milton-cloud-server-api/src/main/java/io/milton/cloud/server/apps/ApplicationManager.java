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
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.manager.MCRootContext;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.MenuItem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import org.apache.velocity.context.InternalContextAdapter;

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
        if (rootFolder == null) {
            return apps;
        } else {
            List<Application> active = (List<Application>) rootFolder.getAttributes().get("activeApps");
            if (active == null) {
                active = findActiveApps(rootFolder);
                log.info("init active apps for: " + rootFolder.getClass() + " = " + active.size());
                rootFolder.getAttributes().put("activeApps", active);
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
            Resource child = app.getPage(parent, name);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        for (Application app : getActiveApps()) {
            app.addBrowseablePages(parent, children);
        }
    }

    public File getAppsConfigDir() {
        return appsConfigDir;
    }

    public void setAppsConfigDir(File appsConfigDir) {
        this.appsConfigDir = appsConfigDir;
    }

    /**
     * TODO: make this read from per-app properties
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
            return new AppConfig(props, rootContext);
        } else {
            AppConfig config = new AppConfig(props, rootContext);
            if (app instanceof LifecycleApplication) {
                ((LifecycleApplication) app).initDefaultProperties(config);
                try (FileOutputStream fout = new FileOutputStream(configFile)) {
                    props.store(fout, "auto-generated defaults");
                }
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
            return findActiveApps(wrf.getWebsite());
        } else {
            Organisation org = rootFolder.getOrganisation();
            return findActiveApps(org);
        }
    }

    public List<Application> findActiveApps(io.milton.vfs.db.Website website) {
        List<AppControl> list = AppControl.find(website, SessionManager.session());
        List<Application> activApps = new ArrayList<>();
        for (AppControl appC : list) {
            if (appC.isEnabled()) {
                Application app = get(appC.getName());
                if (app != null) {
                    activApps.add(app);
                }
            }
        }
        return activApps;
    }

    public List<Application> findActiveApps(Organisation org) {
        if (org.getOrganisation() == null) {
            return apps;
        }

        List<AppControl> list = AppControl.find(org, SessionManager.session());
        List<Application> activApps = new ArrayList<>();
        for (AppControl appC : list) {
            if (appC.isEnabled()) {
                Application app = get(appC.getName());
                if (app != null) {
                    activApps.add(app);
                }
            }
        }
        return activApps;
    }

    public boolean isActive(Application aThis, Website website) {
        List<Application> activeApps = findActiveApps(website);
        for( Application a : activeApps ) {
            if( a.getInstanceId().equals(aThis.getInstanceId())) {
                return true;
            }
        }
        return false;
    }

    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, org.apache.velocity.context.Context context, Writer writer) throws IOException {
        for (Application app : getActiveApps()) {
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
    
    
}
