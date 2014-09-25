package org.spliffy.sync.app;

import io.milton.common.Path;
import io.milton.event.EventManager;
import io.milton.event.EventManagerImpl;
import io.milton.sync.SpliffySync;
import io.milton.sync.SyncCommand;
import io.milton.sync.SyncJob;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SyncFactory {

    private static final Logger log = LoggerFactory.getLogger(SyncFactory.class);
    private static SyncFactory moduleFactory;

    public static SyncFactory get() throws Exception {
        if (moduleFactory == null) {
            moduleFactory = new SyncFactory();
        }
        return moduleFactory;
    }
    private final TrayController trayController;
    private final WindowController windowController;
    private final EventManager eventManager;
    private boolean runningInSystemTray = false;

    public SyncFactory() throws Exception {
        File fUserDir = new File(System.getProperty("user.home"));
        log.info("user home: " + fUserDir.getCanonicalPath());
        File fConfigDir = new File(fUserDir, ".milton-sync");
        if (!fConfigDir.exists()) {
            fConfigDir.mkdir();
        }
        log.info("config dir: " + fConfigDir.getCanonicalPath());
        File fConfigFile = new File(fConfigDir, "config.properties");
        System.out.println("Load config file: " + fConfigFile.getCanonicalPath());
        Properties props = new Properties();
        if (fConfigFile.exists()) {
            FileInputStream fin = new FileInputStream(fConfigFile);
            props.load(fin);
        }

        Map<String, SyncJob> mapOfJobs = new HashMap<String, SyncJob>();
        for (String key : props.stringPropertyNames()) {
            if (key.contains(".")) {
                String[] parts = key.split("[.]");
                String jobId = parts[0];
                SyncJob job = mapOfJobs.get(jobId);
                if (job == null) {
                    job = new SyncJob();
                    job.setMonitor(true);
                    mapOfJobs.put(jobId, job);
                }
                String prop = parts[1];
//        this.localDir = localDir;
//        this.remoteAddress = sRemoteAddress;
//        this.user = user;
//        this.pwd = pwd;
//        this.monitor = monitor;
//        this.localReadonly = readonlyLocal;            
                if (prop.equals("localDir")) {
                    String s = props.getProperty(key);
                    File dir = new File(s);           
                    System.out.println("sync dir: " + s + " -->> " + dir.getCanonicalPath() + " == " + dir.exists());
                    job.setLocalDir(dir);
                } else if (prop.equals("remoteAddress")) {
                    job.setRemoteAddress(props.getProperty(key));
                } else if (prop.equals("user")) {
                    job.setUser(props.getProperty(key));
                } else if (prop.equals("pwd")) {
                    job.setPwd(props.getProperty(key));
                } else {
                    log.warn("Unknown property: " + prop);
                }
            }
        }
                
        File dbFile = new File(fConfigDir, "db");
        eventManager = new EventManagerImpl();
        List<SpliffySync> syncers = SyncCommand.start(dbFile, mapOfJobs.values(), eventManager);
        if (syncers.size() > 0) {
            SpliffySync syncer = syncers.get(0);
            String href = syncer.getHttpClient().getHref(Path.root);
            windowController = new WindowController(href); // TODO: need links for each job
            trayController = new TrayController(eventManager, windowController, syncer);
            runningInSystemTray = trayController.show();
        } else {
            windowController = new WindowController(null);
            trayController = new TrayController(eventManager, windowController, null);
            runningInSystemTray = trayController.show();
        }
    }

    public void startAll() {
        System.out.println("SpliffySync: startAll");
        try {
            windowController.hideMain();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public static SyncFactory getModuleFactory() {
        return moduleFactory;
    }

    public TrayController getTrayController() {
        return trayController;
    }

    public WindowController getWindowController() {
        return windowController;
    }
    
    
    
}
