package io.milton.cloud.server.db.utils;

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
import io.milton.resource.AccessControlledResource;

/**
 *
 * @author brad
 */
public class InitialDataCreator {

    private final SessionFactory sessionFactory;
    private final PasswordManager passwordManager;
    private boolean enabled = false;
    private String rootOrgName = "rootOrg";
    private String initialWebsite = "localhost";
    private String adminUserName = "admin";
    private String adminPassword = "password8";

    public InitialDataCreator(SessionFactory sessionFactory, PasswordManager passwordManager) {
        this.sessionFactory = sessionFactory;
        this.passwordManager = passwordManager;
    }

    /**
     * Can be called from spring init-method
     *
     */
    public void initTestData() {
        SessionManager sessionManager = new SessionManager(sessionFactory);
        Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();
        
        GroupDao groupDao = new GroupDao();
        Organisation rootOrg = OrganisationDao.getRootOrg(session);
        if( rootOrg == null ) {
            System.out.println("Create new organisation");
            rootOrg = new Organisation();
            rootOrg.setName(rootOrgName);
            rootOrg.setModifiedDate(new Date());
            rootOrg.setCreatedDate(new Date());            
            session.save(rootOrg);
            Website spliffyWeb = new Website();            
            spliffyWeb.setBaseEntity(rootOrg);
            spliffyWeb.setName(initialWebsite);
            spliffyWeb.setCreatedDate(new Date());
            spliffyWeb.setTheme("yellow");
            session.save(spliffyWeb);
            
            createChildOrg(rootOrg, "Branch1", session);
            createChildOrg(rootOrg, "Branch2", session);
            createChildOrg(rootOrg, "Branch3", session);
        }
        Group administrators = checkCreateGroup(rootOrg, Group.ADMINISTRATORS,groupDao, session);
        administrators.grant(AccessControlledResource.Priviledge.READ, rootOrg);
        administrators.grant(AccessControlledResource.Priviledge.READ_ACL, rootOrg);
        administrators.grant(AccessControlledResource.Priviledge.WRITE, rootOrg);
        administrators.grant(AccessControlledResource.Priviledge.WRITE_ACL, rootOrg);
        
        Group users = checkCreateGroup(rootOrg, Group.USERS,groupDao, session);
        
        checkCreateUser(adminUserName, adminPassword,session, rootOrg).addToGroup(administrators).addToGroup(users);        
        checkCreateUser("user1", "password1",session, rootOrg).addToGroup(users);
        checkCreateUser("user2", "password1",session, rootOrg).addToGroup(users);
        checkCreateUser("user3", "password1",session, rootOrg).addToGroup(users);
        tx.commit();
        session.close();
        sessionManager.close();
    }

    private Group checkCreateGroup(Organisation org, String name, GroupDao groupDao, Session session) throws HibernateException {
        Group g = groupDao.findGroup(org, name, session);
        if( g == null ) {
            g = new Group();
            g.setName(name);
            g.setOrganisation(org);
            g.setCreatedDate(new Date());
            g.setModifiedDate(new Date());
            session.save(g);
        }
        return g;
    }

    private Profile checkCreateUser(String name,String password, Session session, Organisation org) throws HibernateException {
        Profile t = Profile.find(org, name, session);
        if (t == null) {
            System.out.println("User not found: " + name + " in org: " + org.getId());
            t = new Profile();
            t.setOrganisation(org);
            t.setName(name);            
            t.setCreatedDate(new Date());
            t.setModifiedDate(new Date());
            t.setEmail(name + "@spliffy.org");
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
}
