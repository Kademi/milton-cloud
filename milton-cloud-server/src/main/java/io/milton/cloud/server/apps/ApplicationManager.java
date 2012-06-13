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

import io.milton.cloud.server.web.templating.MenuItem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import io.milton.cloud.server.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

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
    private File appsConfigDir;

    public ApplicationManager(List<Application> initialApps) {
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
        for( Application app : apps ) {
            if( app.getInstanceId().equals(name)) {
                return app;
            }
        }
        return null;
    }

    public List<MenuItem> getMenu(Resource r, Profile user, RootFolder rootFolder) {
        List<MenuItem> list = new ArrayList<>();
        for (Application app : apps) {
            app.appendMenu(list, r, user, rootFolder);
        }
        return list;
    }

    public void shutDown() {
        for (Application app : apps) {
            app.shutDown();
        }
    }

    public Resource getPage(Resource parent, String name) {
        for (Application app : apps) {
            Resource child = app.getPage(parent, name);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    public void addBrowseablePages(CollectionResource parent, List<Resource> children) {
        for (Application app : apps) {
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
            return new AppConfig(props);
        } else {
            AppConfig config = new AppConfig(props);
            app.initDefaultProperties(config);
            try (FileOutputStream fout = new FileOutputStream(configFile)) {
                props.store(fout, "auto-generated defaults");
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
}
