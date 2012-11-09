package io.milton.cloud.server.init;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.ManageWebsiteBranchFolder;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.event.WebsiteCreatedEvent;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.event.EventManager;
import io.milton.sync.event.EventUtils;
import io.milton.vfs.db.*;
import java.io.IOException;
import java.util.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class InitHelper {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(InitHelper.class);
    private final ApplicationManager applicationManager;
    private final PasswordManager passwordManager;
    private List<String> names = new ArrayList<>();

    public InitHelper(PasswordManager passwordManager, ApplicationManager applicationManager) {
        this.passwordManager = passwordManager;
        this.applicationManager = applicationManager;
        addRandomNames();
    }

    public EmailTrigger createTrigger(Organisation org, String eventId, String name, String html, String triggerCondition1, String triggerCondition2, String triggerCondition3, String triggerCondition4, Session session) {
        EmailTrigger t = new EmailTrigger();
        t.setOrganisation(org);
        t.setEventId(eventId);
        t.setName(name);
        t.setEnabled(true);
        t.setFromAddress("admin@localhost");
        t.setHtml(html);
        t.setIncludeUser(true);
        t.setSubject("Test message");
        t.setTitle("Test message");
        t.setTriggerCondition1(triggerCondition1);
        t.setTriggerCondition2(triggerCondition2);
        t.setTriggerCondition3(triggerCondition3);
        t.setTriggerCondition4(triggerCondition4);
        session.save(t);
        return t;
    }

    /**
     *
     * @param org
     * @param name
     * @param groupDao
     * @param numUsers
     * @param session
     * @param emailSender
     * @param registrationMode = eg "o" = open
     * @return
     * @throws HibernateException
     */
    public Group checkCreateGroup(Organisation org, String name, int numUsers, Session session, Profile emailSender, String registrationMode) throws HibernateException {
        Group g = Group.findGroup(org, name, session);
        if (g == null) {
            g = new Group();
            g.setOrganisation(org);
            g.setName(name);
            g.setCreatedDate(new Date());
            g.setModifiedDate(new Date());
            g.setRegistrationMode(registrationMode);
            if (org.getGroups() == null) {
                org.setGroups(new ArrayList<Group>());
            }
            org.getGroups().add(g);
            session.save(g);

            for (int i = 0; i < numUsers; i++) {
                String pname = name + i;
                Profile p = checkCreateUser(pname, "password8", session, org, emailSender);
                p.addToGroup(g, org, session);
            }
        }
        return g;
    }

    /**
     * Check that the given group has the roles (on their own organisation) and
     * create if missing
     *
     * @param group
     * @param roles
     */
    public void checkGroupRoles(Group group, Session session, String... roles) {
        if (group.getGroupRoles() == null) {
            group.setGroupRoles(new ArrayList<GroupRole>());
        }
        for (String r : roles) {
            if (!group.hasRole(r)) {
                group.grantRole(r, session);
            }
        }
    }

    public void createGroupEmailJobs(Organisation o, Session session, Group... groups) {
        // create a few group emails
        for (int i = 1; i < 5; i++) {
            GroupEmailJob j = new GroupEmailJob();
            j.setOrganisation(o);
            j.setFromAddress("joe@somewhere.com");
            j.setName("test-job-" + i);
            j.setStatusDate(new Date());
            j.setSubject("Test email " + i);
            j.setTitle("A test email " + i);
            session.save(j);

            for (Group g : groups) {
                GroupRecipient gr = new GroupRecipient();
                gr.setJob(j);
                gr.setRecipient(g);
                session.save(gr);
            }

        }

    }

    /**
     * Creates the profile and, optionally, can populate the user account with a
     * bnuch of emails
     *
     * @param name
     * @param password
     * @param session
     * @param org
     * @param emailSender
     * @return
     * @throws HibernateException
     */
    public Profile checkCreateUser(String name, String password, Session session, Organisation org, Profile emailSender) throws HibernateException {
        Profile be = Profile.find(name, session);
        if (be == null) {
            Profile t = new Profile();
            t.setName(name);
            t.setNickName(name);
            t.setCreatedDate(new Date());
            t.setModifiedDate(new Date());
            t.setEmail(name + "@fuselms.com");
            session.save(t);
            passwordManager.setPassword(t, password);

            // create emails
            if (emailSender != null) {
                for (int i = 1; i < 10; i++) {
                    EmailItem e = new EmailItem();
                    e.setCreatedDate(new Date());
                    e.setFromAddress("admin@fuselms.com");
                    e.setHtml("<p>Hi there! test message " + i + "</p>");
                    e.setText("Hi there! test message " + i);
                    e.setRecipient(t);
                    e.setRecipientAddress(t.getEmail());
                    e.setReplyToAddress("admin@fuselms.com");
                    e.setSendStatus("c");
                    e.setSendStatusDate(new Date());
                    e.setSender(emailSender);
                    e.setSubject("Test message " + i);
                    session.save(e);
                }
            }
            return t;
        }
        return (Profile) be;
    }

    public String randomName() {
        int item = new Random().nextInt(names.size());
        return names.get(item);
    }

    public void addRandomNames() {
        String[] arr = {"Joe", "Steve", "Mary", "Sally", "John", "Jones", "Smith", "Friday", "Saturn", "Bill", "Dave", "Jeeves"};
        names.addAll(Arrays.asList(arr));
    }

    public String randomPhoneNum() {
        int item = new Random().nextInt(9999999);
        return item + "";
    }

    public Website checkCreateWebsite(Session session, Organisation org, String name, String dnsName, String theme, Profile user, String... aliases) {
        for (Website w : org.websites()) {
            if (w.getName().equals(name)) {
                return w;
            }
        }

        Website mainWebsite = org.createWebsite(name, dnsName, theme, user, session);

        Repository r = mainWebsite;
        r.setPublicContent(true); // allow public access

        for (String alias : aliases) {
            System.out.println("-- Create website: " + dnsName + " with alias: " + alias);
            mainWebsite.createAlias(alias, session);
        }

        return mainWebsite;
    }

    public void enableAllApps(Organisation org, Profile user, Session session) {
        for (Application app : applicationManager.getApps()) {
            AppControl.setStatus(app.getInstanceId(), org, true, user, new Date(), session);
        }
    }

    public void enableAllApps(Branch websiteBranch, Profile user, Session session) {
        Organisation org = (Organisation) websiteBranch.getRepository().getBaseEntity();
        enableAllApps(org, user, session);
        for (Application app : applicationManager.getApps()) {
            AppControl.setStatus(app.getInstanceId(), websiteBranch, true, user, new Date(), session);
        }
    }

    public void enableApps(Branch websiteBranch, Profile user, Session session, String... appIds) {
        for (String appId : appIds) {
            Application app = applicationManager.get(appId);
            if (app != null) {
                AppControl.setStatus(app.getInstanceId(), websiteBranch, true, user, new Date(), session);
            } else {
                log.error("App not found: " + appId);
            }
        }
    }

    public Organisation createChildOrg(Organisation rootOrg, String branch1, Session session) {
        return rootOrg.createChildOrg(branch1, session);
    }

    public Website createAndPopulateWebsite(Session session, Organisation org, String newName, String newDnsName, Profile curUser, CommonCollectionResource parent) throws IOException {
        Website website = org.createWebsite(newName, newDnsName, null, curUser, session);
        Branch websiteBranch = website.liveBranch();
        Repository r = website;
        r.setPublicContent(true); // allow public access
        session.save(r);

        Map<String, String> themeParams = new HashMap<>();
        themeParams.put("frameworkToolsColor", "#FFF");
        themeParams.put("frameworkToolsInverseColor", " #414141");
        themeParams.put("frameworkHighlightColor", " #FFF");
        themeParams.put("frameworkHighlightInverseColor", " #FF9531");

        themeParams.put("hero1", "#88c03f");
        themeParams.put("hero2", "#88c03f");
        themeParams.put("text1", "#1C1D1F");
        themeParams.put("text2", "#2F2F2F");
        themeParams.put("border1", "#DBDADA");
        themeParams.put("bg1", "#2F2F2F");
        themeParams.put("borderRadius", "5px");
        themeParams.put("listIndent", "40px");

        Map<String, String> themeAttributes = new HashMap<>();
        themeAttributes.put("logo", website.getName());

        ManageWebsiteBranchFolder websiteBranchFolder = new ManageWebsiteBranchFolder(website, websiteBranch, parent);

        websiteBranchFolder.setThemeParams(themeParams);
        websiteBranchFolder.setThemeAttributes(themeAttributes);
        websiteBranchFolder.save();

        Date now = _(CurrentDateService.class).getNow();
        AppControl.initDefaultApps(websiteBranch, curUser, now, session);
        EventUtils.fireQuietly(_(EventManager.class), new WebsiteCreatedEvent(website));
        return website; 
    }
}
