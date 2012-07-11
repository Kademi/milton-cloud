package io.milton.cloud.server.init;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.db.utils.GroupDao;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Contact;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.utils.SessionManager;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import java.util.Date;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
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
    private String initialWebsite;
    private String adminUserName;
    private String adminPassword;
    private List<String> names = new ArrayList<>();
    private ApplicationManager applicationManager;



    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        initialRootOrgName = config.get("initialRootOrgName");
        initialWebsite = config.get("initialWebsite");
        adminUserName = config.get("adminUserName");
        adminPassword = config.get("adminPassword");
        this.sessionManager = resourceFactory.getSessionManager();
        this.passwordManager = resourceFactory.getSecurityManager().getPasswordManager();
        this.applicationManager = resourceFactory.getApplicationManager();

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

            createChildOrg(rootOrg, "Branch1", session);
            createChildOrg(rootOrg, "Branch2", session);
            createChildOrg(rootOrg, "Branch3", session);
        } else {
            System.out.println("Root org exists: " + rootOrg.getName());
        }

        Profile admin = checkCreateUser(adminUserName, adminPassword, session, rootOrg);


        Group administrators = checkCreateGroup(rootOrg, Group.ADMINISTRATORS, groupDao, session);
        administrators.grant(AccessControlledResource.Priviledge.READ, rootOrg, session);
        administrators.grant(AccessControlledResource.Priviledge.READ_ACL, rootOrg, session);
        administrators.grant(AccessControlledResource.Priviledge.WRITE, rootOrg, session);
        administrators.grant(AccessControlledResource.Priviledge.WRITE_ACL, rootOrg, session);

        Group users = checkCreateGroup(rootOrg, Group.USERS, groupDao, session);        
        if (newOrg) {
            Website spliffyWeb = rootOrg.createWebsite("localhost", "fuse", admin, session);
            spliffyWeb.addGroup(users, "o", session);
        }        

        admin.addToGroup(administrators).addToGroup(users);
        checkCreateUser("user1", "password1", session, rootOrg).addToGroup(users);
        checkCreateUser("user2", "password1", session, rootOrg).addToGroup(users);
        checkCreateUser("user3", "password1", session, rootOrg).addToGroup(users);
        tx.commit();
        session.close();
        sessionManager.close();
    }

    private Group checkCreateGroup(Organisation org, String name, GroupDao groupDao, Session session) throws HibernateException {
        Group g = groupDao.findGroup(org, name, session);
        if (g == null) {
            g = new Group();
            g.setName(name);
            g.setOrganisation(org);
            g.setCreatedDate(new Date());
            g.setModifiedDate(new Date());
            session.save(g);
        }
        return g;
    }

    private Profile checkCreateUser(String name, String password, Session session, Organisation org) throws HibernateException {
        Profile t = Profile.find(org, name, session);
        if (t == null) {
            System.out.println("User not found: " + name + " in org: " + org.getId());
            t = new Profile();
            t.setOrganisation(org);
            t.setName(name);
            t.setCreatedDate(new Date());
            t.setModifiedDate(new Date());
            t.setEmail(name + "@spliffy.org");
            t.setAdminOrg(org);
            session.save(t);
            passwordManager.setPassword(t, password);
            System.out.println("created test user");

            Repository r1 = new Repository();
            r1.setBaseEntity(t);
            r1.setCreatedDate(new Date());
            r1.setName("repo1");
            session.save(r1);

            Calendar cal = new Calendar();
            cal.setOwner(t);
            cal.setCreatedDate(new Date());
            cal.setCtag(System.currentTimeMillis());
            cal.setModifiedDate(new Date());
            cal.setName("cal1");
            session.save(cal);

//            CalEvent e =new CalEvent();
//            e.setCalendar(cal);
//            e.setCreatedDate(new Date());
//            e.setCtag(System.currentTimeMillis());
//            e.setDescription("Auto generated event");
//            e.setModifiedDate(new Date());
//            e.setName("Auto1");
//            e.setStartDate(new Date());
//            e.setEndDate(new Date( e.getStartDate().getTime() + 1000*60*60*3 )); // 3 hours later            
//            e.setSummary("Some summary goes here");
//            e.setTimezone(TimeZone.getDefault().getID()); // this ruight??
//            session.save(e);
//            
            AddressBook addressBook = new AddressBook();
            addressBook.setName("contacts");
            addressBook.setOwner(t);
            addressBook.setCreatedDate(new Date());
            addressBook.setModifiedDate(new Date());
            addressBook.setDescription("Auto generated");
            session.save(addressBook);

            Contact c = new Contact();
            c.setName("contact1");
            c.setAddressBook(addressBook);
            c.setCreatedDate(new Date());
            c.setModifiedDate(new Date());
            c.setGivenName("Joe");
            c.setSurName("Bloggs");
            c.setTelephonenumber("555 1234");
            c.setMail("joe@blogs.com");
            c.setOrganizationName("Bloggs.com");
            c.setUid(UUID.randomUUID().toString());
            session.save(c);
        }
        return t;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void createChildOrg(Organisation rootOrg, String branch1, Session session) {
        Organisation o = new Organisation();
        o.setOrganisation(rootOrg);
        o.setName(branch1);
        o.setCreatedDate(new Date());
        o.setModifiedDate(new Date());
        session.save(o);
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
