package io.milton.cloud.server.init;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.db.AppControl;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import io.milton.vfs.db.Profile;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.event.EventManager;
import io.milton.event.EventManagerImpl;
import io.milton.sync.SyncCommand;
import io.milton.sync.SyncJob;
import io.milton.vfs.db.Branch;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author brad
 */
public class InitialDataCreator implements LifecycleApplication {

    private SessionManager sessionManager;
    private PasswordManager passwordManager;
    private boolean enabled = false;
    private String initialRootOrgName;
    private String adminUserName;
    private String adminPassword;
    private List<String> names = new ArrayList<>();
    private ApplicationManager applicationManager;
    private InitHelper initHelper;

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        System.out.println("--- Initial Data ---");
        initialRootOrgName = config.get("initialRootOrgName");
        adminUserName = config.get("adminUserName");
        adminPassword = config.get("adminPassword");
        this.sessionManager = resourceFactory.getSessionManager();
        this.passwordManager = resourceFactory.getSecurityManager().getPasswordManager();
        this.applicationManager = resourceFactory.getApplicationManager();

        this.initHelper = new InitHelper(passwordManager, applicationManager);

        initTestData();
        initContentAutoLoad();
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Initial data creator";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Runs on startup and checks that a minimum core set of data is present in the database";
    }

    /**
     * Can be called from spring init-method
     *
     */
    public void initTestData() {
        System.out.println("Create initial data...");
        Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();

        Organisation rootOrg = Organisation.getRootOrg(session);
        if (rootOrg != null) {
            System.out.println("Root org exists, do nothing");
            return;
        }
        System.out.println("Create new organisation");
        rootOrg = new Organisation();
        rootOrg.setOrgId(initialRootOrgName);
        rootOrg.setModifiedDate(new Date());
        rootOrg.setCreatedDate(new Date());
        session.save(rootOrg);

        Profile admin = initHelper.checkCreateUser(adminUserName, adminPassword, session, rootOrg, null);

        for (Application app : applicationManager.getApps()) {
            AppControl.setStatus(app.getInstanceId(), rootOrg, true, admin, new Date(), session);
        }


        Group administrators = initHelper.checkCreateGroup(rootOrg, Group.ADMINISTRATORS, 0, session, null, "c");
        administrators.grantRole("Administrator", session);
        administrators.grantRole("Content author", session);

        Group users = initHelper.checkCreateGroup(rootOrg, Group.USERS, 0, session, admin, "o");
        users.grantRole("Content author", session);

        Group publicGroup = initHelper.checkCreateGroup(rootOrg, Group.PUBLIC, 0, session, admin, "o");
        publicGroup.grantRole("Forums viewer", session);


        Profile normalUser = initHelper.checkCreateUser("a1", adminPassword, session, rootOrg, null);
        normalUser.addToGroup(users, rootOrg, session);
        if (normalUser.repository("files") == null) {
            normalUser.createRepository("files", normalUser, session);
        }

        admin.addToGroup(administrators, rootOrg, session).addToGroup(users, rootOrg, session);
        Website miltonSite = initHelper.checkCreateWebsite(session, rootOrg, "milton", "milton.io", "milton", admin); // can be accessed on milton.[primary_domain] or milton.io        
        System.out.println("milton site: " + miltonSite.getName() + " - " + miltonSite.getId());

        initHelper.enableApps(miltonSite.getTrunk(), admin, session, "admin", "userApp", "forums", "email", "content", "search", "signup", "userDashboardApp");
        miltonSite.addGroup(users, session);
        String menu = "/content/index.html,Home\n"
                + "/content/maven/index.html,Downloads\n"
                + "/content/guide/index.html,Documentation";

//        miltonSite.setAttribute("logo", "milton.io", session);
//        miltonSite.setAttribute("menu", menu, session);

        AppControl ac = AppControl.find(miltonSite.getTrunk(), "signup", session);
        ac.setSetting("signup.next.href", "/content/gettingStarted.html", session);

        Website myMiltonSite = initHelper.checkCreateWebsite(session, rootOrg, "mymilton", "my.milton.io", "milton", admin); // can be accessed on mymilton.localhost or my.milton.io
        //miltonSite.setAttribute("logo", "my.milton.io", session);
        initHelper.enableApps(myMiltonSite.getTrunk(), admin, session, "admin", "userApp", "myFiles", "calendar", "contacts", "signup", "userDashboardApp");
        myMiltonSite.addGroup(users, session);

        tx.commit();
        session.close();
        sessionManager.close();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
        config.add("initialRootOrgName", "rootOrg");
        config.add("initialWebsite", "localhost");
        config.add("adminUserName", "admin");
        config.add("adminPassword", "password8");
    }

    @Override
    public String getInstanceId() {
        return "initialDataCreator";
    }

    private void initContentAutoLoad() throws Exception {
        System.out.println("-------- Initial Content Auto load ---------------");
        String sRootDir = "../sites";
        File rootDir = new File(sRootDir);
        if (!rootDir.exists()) {
            throw new RuntimeException("Fuse autoloader root directory does not exist: " + rootDir.getAbsolutePath());
        }
        System.out.println("Autoload source path: " + rootDir.getAbsolutePath());

        File miltonContent = new File(rootDir, "milton");
        File mymiltonContent = new File(rootDir, "mymilton");
        File mavenContent = new File(rootDir, "maven");

        System.out.println("Beginning monitor of content dir: " + miltonContent.getAbsolutePath());
        File dbFile = new File("target/sync-db");
        boolean localReadonly = true;
        List<SyncJob> jobs = Arrays.asList(
                new SyncJob(miltonContent, "http://127.0.0.1:8080/milton/" + Branch.TRUNK + "/", "admin", "password8", true, localReadonly),
                new SyncJob(mymiltonContent, "http://127.0.0.1:8080/mymilton/" + Branch.TRUNK + "/", "admin", "password8", true, localReadonly),
                new SyncJob(mavenContent, "http://127.0.0.1:8080/maven/" + Branch.TRUNK + "/", "admin", "password8", true, localReadonly) // load the maven repo with some test data
                );
        EventManager eventManager = new EventManagerImpl();
        SyncCommand.start(dbFile, jobs, eventManager);
    }
}
