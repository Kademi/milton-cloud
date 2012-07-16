package io.milton.cloud.server.init;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.db.utils.GroupDao;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import io.milton.vfs.db.Profile;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import java.util.ArrayList;
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
        initialRootOrgName = config.get("initialRootOrgName");
        adminUserName = config.get("adminUserName");
        adminPassword = config.get("adminPassword");
        this.sessionManager = resourceFactory.getSessionManager();
        this.passwordManager = resourceFactory.getSecurityManager().getPasswordManager();
        this.applicationManager = resourceFactory.getApplicationManager();

        this.initHelper = new InitHelper(passwordManager, applicationManager);

        initTestData();
    }

    /**
     * Can be called from spring init-method
     *
     */
    public void initTestData() {
        Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();

        GroupDao groupDao = new GroupDao();
        Organisation rootOrg = OrganisationDao.getRootOrg(session);
        boolean newOrg = false;
        if (rootOrg == null) {
            System.out.println("Create new organisation");
            newOrg = true;
            rootOrg = new Organisation();
            rootOrg.setName(initialRootOrgName);
            rootOrg.setModifiedDate(new Date());
            rootOrg.setCreatedDate(new Date());
            session.save(rootOrg);
        } else {
            System.out.println("Root org exists: " + rootOrg.getName());
        }

        Profile admin = initHelper.checkCreateUser(adminUserName, adminPassword, session, rootOrg, null);


        Group administrators = initHelper.checkCreateGroup(rootOrg, Group.ADMINISTRATORS, groupDao, 0, session, null);
        administrators.grantRole(GroupRole.ROLE_ADMIN, true, session);
        administrators.grantRole(GroupRole.ROLE_AUTHOR, true, session);

        Group users = initHelper.checkCreateGroup(rootOrg, Group.USERS, groupDao, 50, session, admin);
        users.grantRole(GroupRole.ROLE_AUTHOR, true, session);

        admin.addToGroup(administrators).addToGroup(users);
        Website miltonSite = initHelper.checkCreateWebsite(session, rootOrg, "milton.io", "fuse", admin, "milton"); // can be accessed on milton.localhost or milton.io
        initHelper.enableApps(miltonSite, admin, session, "admin", "users", "organisations", "website", "forums", "email");
        miltonSite.addGroup(users, "o", session);

        Website myMiltonSite = initHelper.checkCreateWebsite(session, rootOrg, "my.milton.io", "fuse", admin, "mymilton"); // can be accessed on mymilton.localhost or my.milton.io
        initHelper.enableApps(myMiltonSite, admin, session, "admin", "users", "organisations", "website", "myFiles", "calendar", "contacts", "email");
        myMiltonSite.addGroup(users, "o", session);

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

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }
}
