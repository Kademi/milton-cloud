package org.spliffy.sync.app;

import io.milton.event.EventManager;
import io.milton.event.EventManagerImpl;
import io.milton.httpclient.Host;
import java.io.File;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spliffy.sync.*;

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

    private final EventManager eventManager;
    private final TrayController trayController;
    private final WindowController windowController;
    private final SpliffySync spliffySync;
    
    private boolean runningInSystemTray = false;

    public SyncFactory() throws Exception {        
        String sLocalDir = "E:\\spliffy-test";
        String sRemoteAddress = "http://localhost:8080/user1/repo1";
        String user = "user1";
        String pwd = "password1";
        
        eventManager = new EventManagerImpl();
        
        File localRootDir = new File(sLocalDir);
        URL url = new URL(sRemoteAddress);
        //HttpClient client = createHost(url, user, pwd);
        
        Host client = new Host(url.getHost(), url.getPort(), user, pwd, null);
        boolean secure = url.getProtocol().equals("https");
        client.setSecure(secure);
        

        System.out.println("Sync: " + localRootDir.getAbsolutePath() + " - " + sRemoteAddress);

        File dbFile = new File("target/sync-db");
        System.out.println("Using database: " + dbFile.getAbsolutePath());

        DbInitialiser dbInit = new DbInitialiser(dbFile);

        JdbcHashCache fanoutsHashCache = new JdbcHashCache(dbInit.getUseConnection(), dbInit.getDialect(), "h");
        JdbcHashCache blobsHashCache = new JdbcHashCache(dbInit.getUseConnection(), dbInit.getDialect(), "b");

        HttpHashStore httpHashStore = new HttpHashStore(client, fanoutsHashCache);
        httpHashStore.setBaseUrl("/_hashes/fanouts/");
        HttpBlobStore httpBlobStore = new HttpBlobStore(client, blobsHashCache);
        httpBlobStore.setBaseUrl("/_hashes/blobs/");

        Archiver archiver = new Archiver();
        
        Syncer syncer = new Syncer(eventManager, localRootDir, httpHashStore, httpBlobStore, client, archiver, url.getPath());
        spliffySync = new SpliffySync(localRootDir, client, url.getPath(), syncer, archiver, dbInit, eventManager);
        
        windowController = new WindowController(sRemoteAddress);
        trayController = new TrayController( eventManager, windowController, spliffySync);
        
        runningInSystemTray = trayController.show();
    }

    public void startAll() {
        System.out.println("SpliffySync: startAll");
        try {
            windowController.hideMain();
            spliffySync.start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
