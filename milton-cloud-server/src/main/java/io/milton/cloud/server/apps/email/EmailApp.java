/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.mail.GroupEmailService;
import io.milton.cloud.server.mail.BatchEmailService;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.EmailApplication;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.db.TriggerTimer;
import io.milton.cloud.server.event.TriggerEvent;
import io.milton.cloud.server.mail.DefaultFilterScriptEvaluator;
import io.milton.cloud.server.mail.FilterScriptEvaluator;
import io.milton.cloud.server.mail.MiltonCloudMailResourceFactory;
import io.milton.cloud.server.mail.XmlScriptParser;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.queue.Processable;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TextTemplater;
import io.milton.mail.MailServer;
import io.milton.mail.MailServerBuilder;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.Context;

import static io.milton.context.RequestContext._;
import io.milton.context.RootContext;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.mail.*;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.config.ConfigurationMBean;
import org.masukomi.aspirin.core.listener.ListenerManager;

/**
 *
 * @author brad
 */
public class EmailApp implements MenuApplication, LifecycleApplication, PortletApplication, EventListener, EmailApplication, ChildPageApplication, BrowsableApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmailApp.class);
    private MiltonCloudMailResourceFactory mailResourceFactory;
    private MailServer mailServer;
    private SpliffySecurityManager securityManager;
    private BatchEmailService batchEmailService;
    private GroupEmailService groupEmailService;
    private Configuration aspirinConfiguration;
    private ListenerManager listenerManager = new ListenerManager();
    private FilterScriptEvaluator filterScriptEvaluator;
    private EmailItemQueueStore queueStore;
    private EmailItemMailStore mailStore;
    private CurrentDateService currentDateService;
    private MCMailFilter mailFilter;
    private EventManager eventManager;
    private AsynchProcessor asynchProcessor;
    private EmailTriggerService emailTriggerService;
    private HashStore hashStore;
    private BlobStore blobStore;
    private RootContext rootContext;
    private SessionManager sessionManager;
    private int smtpPort = 2525;
    private long scanTriggersPeriod = 1000 * 60; // * 60;

    @Override
    public String getInstanceId() {
        return "email";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Email";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides end users with an email inbox, and allows administrators to send emails to groups of users";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        smtpPort = config.getInt("smtp.port");

        this.rootContext = config.getContext();
        this.sessionManager = resourceFactory.getSessionManager();
        Properties props = new Properties();
        String hostName = config.getContext().get(CurrentRootFolderService.class).getPrimaryDomain();
        props.setProperty(ConfigurationMBean.PARAM_HOSTNAME, hostName);
        aspirinConfiguration = new Configuration(props);
        aspirinConfiguration.setDeliveryThreadsActiveMax(20);  // up from default of 3
        XmlScriptParser xmlScriptParser = new XmlScriptParser(resourceFactory.getApplicationManager());
        config.getContext().put(xmlScriptParser);
        filterScriptEvaluator = new DefaultFilterScriptEvaluator(xmlScriptParser, currentDateService, config.getContext().get(Formatter.class));
        batchEmailService = new BatchEmailService(filterScriptEvaluator, resourceFactory.getApplicationManager());
        config.getContext().put(batchEmailService);
        groupEmailService = new GroupEmailService(batchEmailService);
        config.getContext().put(groupEmailService);
        securityManager = resourceFactory.getSecurityManager();
        mailResourceFactory = new MiltonCloudMailResourceFactory(resourceFactory);
        this.currentDateService = config.getContext().get(CurrentDateService.class);
        queueStore = new EmailItemQueueStore(resourceFactory.getSessionManager(), aspirinConfiguration, listenerManager, currentDateService);
        StandardMessageFactory smf = new StandardMessageFactoryImpl();
        this.hashStore = config.getContext().get(HashStore.class);
        this.blobStore = config.getContext().get(BlobStore.class);
        mailStore = new EmailItemMailStore(resourceFactory.getSessionManager(), smf, hashStore, blobStore, rootContext);
        mailFilter = new MCMailFilter(resourceFactory.getSessionManager(), config.getContext());
        emailTriggerService = new EmailTriggerService(batchEmailService, filterScriptEvaluator, resourceFactory.getApplicationManager());
        config.getContext().put(emailTriggerService);

        resourceFactory.getApplicationManager().getEmailTriggerTypes().add(new SubscriptionEventTriggerType());

        MailServerBuilder mailServerBuilder = new MailServerBuilder();
        mailServerBuilder.setListenerManager(listenerManager);
        mailServerBuilder.setAspirinConfiguration(aspirinConfiguration);
        mailServerBuilder.setMailResourceFactory(mailResourceFactory);
        mailServerBuilder.setEnablePop(false);
        mailServerBuilder.setEnableMsa(false);
        mailServerBuilder.setSmtpPort(smtpPort);
        mailServerBuilder.setMailStore(mailStore);
        mailServerBuilder.setQueueStore(queueStore);
        List<Filter> filters = new ArrayList<>();
        filters.add(mailFilter);
        mailServerBuilder.setFilters(Collections.unmodifiableList(filters));
        mailServer = mailServerBuilder.build();
        mailStore.setAspirinInternal(mailServerBuilder.getAspirinInternal());
        mailServer.start();

        eventManager = config.getContext().get(EventManager.class);
        eventManager.registerEventListener(this, TriggerEvent.class);

        asynchProcessor = config.getContext().get(AsynchProcessor.class);
        asynchProcessor.schedule(new ScanTriggerTimersProcess(), scanTriggersPeriod);
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            if (requestedName.equals("inbox")) {
                return new EmailFolder(wrf, requestedName, null);
            }
        } else if (parent instanceof ManageGroupEmailFolder) {
            if (requestedName.equals("log.html")) {
                ManageGroupEmailFolder f = (ManageGroupEmailFolder) parent;
                return new ManageGroupEmailLog(requestedName, f, f.getJob());
            }

        } else if (parent instanceof ManageAutoEmailFolder) {
            if (requestedName.equals("timerTriggers.html")) {
                ManageAutoEmailFolder f = (ManageAutoEmailFolder) parent;
                return new ViewTriggerTimersPage(requestedName, f);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder orgFolder = (OrganisationFolder) parent;
            children.add(new ManageGroupEmailsFolder("groupEmails", orgFolder, orgFolder.getOrganisation()));
            children.add(new ManageAutoEmailsFolder("autoEmails", orgFolder, orgFolder.getOrganisation()));
        }
        if (parent instanceof BaseEntityResource) {
            BaseEntityResource ur = (BaseEntityResource) parent;
            EmailFolder f = new EmailFolder(ur, "inbox", ur.getBaseEntity());
            children.add(f);
        }
    }

    @Override
    public void appendMenu(MenuItem parent) {
        if (_(SpliffySecurityManager.class).getCurrentUser() == null) {
            return;
        }
        switch (parent.getId()) {
            case "menuRoot":
                if (parent.getRootFolder() instanceof WebsiteRootFolder) {
                    parent.getOrCreate("menuNotifications", getEmailMenuItemText(), "/inbox").setOrdering(50);
                } else {
                    parent.getOrCreate("menuTalk", "Talk &amp; Connect").setOrdering(30);
                }
                break;
            case "menuTalk":
                parent.getOrCreate("menuEmails", "Send emails").setOrdering(20);
                break;
            case "menuEmails":
                OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
                if (parentOrg != null) {
                    parent.getOrCreate("menuSendEmail", "Send and manage emails", parentOrg.getPath().child("groupEmails")).setOrdering(10);
                    parent.getOrCreate("menuAutoEmail", "Manage auto emails", parentOrg.getPath().child("autoEmails")).setOrdering(20);
                }
                break;
        }
    }

    private String getEmailMenuItemText() {
        Profile currentUser = securityManager.getCurrentUser();
        long numUnread = EmailItem.findByNumUnreadByRecipient(currentUser, SessionManager.session());
        StringBuilder sb = new StringBuilder();
        if (numUnread > 0) {
            sb.append("<span>").append(numUnread).append("</span>");
        }
        sb.append("Notifications");
        return sb.toString();
    }

    @Override
    public void shutDown() {
        if (mailServer != null) {
            mailServer.stop();
        }
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
        Integer i = config.getInt("smtp.port");
        if (i == null) {
            i = 2525;
            config.setInt("smtp.port", i);
        }
        int ii = config.getInt("smtp.port");
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (portletSection.equals("dashboardMessages")) {
            _(TextTemplater.class).writePage("email/emailMessagesPortlet.html", currentUser, rootFolder, context, writer);
        }
//            <div>                
//                <div class="txtR">
//                    <h3>Hi Patrick, Welcome to your dashboard!</h3>
//                    <p>Below you will findFromRoot a list of your most recent and active modules.  A panel which gives you an overview of your eLearning progress and a snapshot of what people are talking about in the community.  Happy learning.</p>
//                </div>
//                <div class="clr"></div>
//                <a class="close" href="#">close</a>
//            </div>

    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof TriggerEvent) {
            TriggerEvent se = (TriggerEvent) e;
            checkTriggers(se);
        }
    }

    @Override
    public void storeMail(PrincipalResource principal, MimeMessage mm) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (principal instanceof UserResource) {
            // TODO: user should have option to not receive these emails
            UserResource ur = (UserResource) principal;
            Profile p = ur.getThisUser();
            EmailItem i = new EmailItem();
            Date now = currentDateService.getNow();
            i.setCreatedDate(now);
            i.setRecipient(p);
            i.setRecipientAddress(p.getEmail());
            i.setSendStatusDate(now);

            EmailItemStandardMessage sm = new EmailItemStandardMessage(i, hashStore, blobStore, rootContext, sessionManager);
            StandardMessageFactoryImpl parser = new StandardMessageFactoryImpl();
            parser.toStandardMessage(mm, sm);
            session.save(i);
            tx.commit();
        } else if (principal instanceof GroupInWebsiteFolder) {
            if (isAutoReply(mm)) {
                log.warn("DISCARDING autoreply");
                return;
            }
            // TODO: Should never accept a group email frmo outside of the group
            // Also, should have config option for groups to only accept emails from admins, or not at all
            GroupInWebsiteFolder gr = (GroupInWebsiteFolder) principal;
            GroupEmailJob job = new GroupEmailJob();
            job.addGroupRecipient(gr.getGroup());
            job.setGroupRecipients(new ArrayList<GroupRecipient>());
            job.setName("received-" + System.currentTimeMillis()); // HACK HACK - make a nice name from subject and ensure uniqueness
            GroupEmailStandardMessage sm = new GroupEmailStandardMessage(job);

            if (!isFromMember(sm, gr.getGroup())) {
                log.warn("Received mail from non-group member, discarding..");
                return;
            }

            job.setTitle(job.getSubject());
            job.setStatusDate(currentDateService.getNow());
            session.save(job);
            tx.commit();
        } else {
            throw new RuntimeException("Unsupported recipient type: " + principal.getClass());
        }
    }

    @Override
    public MessageFolder getInbox(PrincipalResource p) {
        try {
            Resource dir = p.child("inbox");
            if (dir instanceof MessageFolder) {
                MessageFolder emailFolder = (MessageFolder) dir;
                return emailFolder;
            } else {
                throw new RuntimeException("inbox folder is not a valid mesasge folder");
            }
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean isAutoReply(MimeMessage mm) {
        String sub = getSubject(mm);
        return isAutoReply(sub);
    }

    private boolean isAutoReply(String sub) {
        sub = sub.toLowerCase();
        if (sub.contains("autoreply")) {
            return true;
        }
        if (sub.contains("out of") && sub.contains("office")) {
            return true;
        }
        return false;
    }

    private String getSubject(MimeMessage mm) {
        try {
            return mm.getSubject();
        } catch (MessagingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void checkTriggers(TriggerEvent event) {
        log.info("checkTriggers");
        Session session = SessionManager.session();
        List<EmailTrigger> triggers = EmailTrigger.find(session, event.getEventId(), event.getWebsite(), event.getTriggerItem1(), event.getTriggerItem2(), event.getTriggerItem3(), event.getTriggerItem4(), event.getTriggerItem5());
        for (EmailTrigger trigger : triggers) {
            log.info("found activated trigger: " + trigger.getEventId());
            enqueueTrigger(trigger, event);
        }
    }

    private void enqueueTrigger(EmailTrigger trigger, TriggerEvent event) {
        log.info("enqueueTrigger: " + trigger.getName() + " - " + event.getEventId());
        Session session = SessionManager.session();

        if (trigger.getCreateTimerEnabled() != null && trigger.getCreateTimerEnabled()) {
            Date now = currentDateService.getNow();            
            // Create a TriggerTimer to refire the event after the specified delay
            TriggerTimer.create(trigger, event.getOrganisation(), event.getWebsite(), event.getSourceProfile(), now, "Created by trigger: " + trigger.getName(), session);

        } else {
            if (emailTriggerService.checkConditions(trigger, event.getSourceProfile())) {
                executeTriggerImmediate(trigger, event, session);
            }
        }
    }

    private void executeTriggerImmediate(EmailTrigger trigger, TriggerEvent event, Session session) {
        if (trigger.getEmailEnabled() == null || trigger.getEmailEnabled()) {
            EmailTriggerProcessable p = new EmailTriggerProcessable(trigger.getId(), event.getSourceProfile().getId());
            asynchProcessor.enqueue(p);
        }
        if (trigger.getAddToGroupEnabled() != null && trigger.getAddToGroupEnabled()) {
            if (trigger.getGroupToJoin() != null) {
                Group groupToJoin = trigger.getGroupToJoin();
                Profile p = event.getSourceProfile();
                p.createGroupMembership(groupToJoin, groupToJoin.getOrganisation(), session);
            }
        }
    }
   

    private boolean isFromMember(GroupEmailStandardMessage sm, Group group) {
        if (sm.getFrom() == null) {
            log.warn("No from address");
            return false;
        }
        Profile p = Profile.findByEmail(sm.getFrom().toPlainAddress(), SessionManager.session());
        if (p == null) {
            log.warn("Received group email from unknown source: " + sm.getFrom().toPlainAddress());
            return false;
        }
        if (!group.isMember(p)) {
            log.warn("Not a group member: " + p.getName());
            return false;
        }
        return true;
    }

    public static class EmailTriggerProcessable implements Processable, Serializable {

        private static final long serialVersionUID = 1l;
        private final long jobId;
        private final Long sourceProfileId;

        public EmailTriggerProcessable(long jobId, long sourceProfileId) {
            this.jobId = jobId;
            this.sourceProfileId = sourceProfileId;
        }

        @Override
        public void doProcess(io.milton.context.Context context) {
            log.info("doProcess: " + jobId);
            Session session = SessionManager.session();
            Transaction tx = session.beginTransaction();
            try {
                EmailTriggerService emailTriggerService = context.get(EmailTriggerService.class);
                if (emailTriggerService == null) {
                    throw new RuntimeException("No " + EmailTriggerService.class + " in context");
                }
                List<Long> sourceEntityIds = Arrays.asList(sourceProfileId);
                emailTriggerService.send(jobId, sourceEntityIds, session);
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                log.error("Exception processing email trigger: " + jobId, e);
            }
        }

        @Override
        public void pleaseImplementSerializable() {
        }
    }

    public class ScanTriggerTimersProcess implements Serializable, Processable {

        private List<Long> dueIds;

        @Override
        public void doProcess(io.milton.context.Context context) {
            Session session = SessionManager.session();
            if (dueIds == null || dueIds.isEmpty()) {
                Transaction tx = session.beginTransaction();
                try {
                    Date now = currentDateService.getNow();
                    dueIds = TriggerTimer.takeDue(now, 10, 5, 3, session);
                    tx.commit();
                } catch (Exception e) {
                    tx.rollback();
                    log.error("Exception processing ScanTriggerTimersProcess", e);
                }
            }

            for (Long id : dueIds) {
                Transaction tx = session.beginTransaction();
                try {
                    TriggerTimer tt = TriggerTimer.get(id, session);
                    if (tt != null) {
                        Date now = currentDateService.getNow();
                        TriggerTimerEvent event = new TriggerTimerEvent(tt);
                        if (emailTriggerService.checkConditions(tt.getEmailTrigger(), event.getSourceProfile())) {
                            executeTriggerImmediate(tt.getEmailTrigger(), event, session);                            
                        }
                        tt.setCompletedProcessingAt(now);
                        session.save(tt);                        
                        tx.commit();
                    }
                } catch (Exception e) {
                    tx.rollback();
                    log.error("Exception processing ScanTriggerTimersProcess", e);
                } finally {
                    session.flush();
                }
            }

        }

        @Override
        public void pleaseImplementSerializable() {
            // ok, done
        }
    }

    public class TriggerTimerEvent implements TriggerEvent {

        private final TriggerTimer triggerTimer;
        private final EmailTrigger emailTrigger;

        public TriggerTimerEvent(TriggerTimer triggerTimer) {
            this.triggerTimer = triggerTimer;
            this.emailTrigger = triggerTimer.getEmailTrigger();
        }

        @Override
        public String getEventId() {
            return emailTrigger.getEventId();
        }

        @Override
        public Organisation getOrganisation() {
            return emailTrigger.getOrganisation();
        }

        @Override
        public Website getWebsite() {
            return triggerTimer.getWebsite();
        }

        @Override
        public Profile getSourceProfile() {
            return triggerTimer.getFireForProfile();
        }

        @Override
        public String getTriggerItem1() {
            return emailTrigger.getTriggerCondition1();
        }

        @Override
        public String getTriggerItem2() {
            return emailTrigger.getTriggerCondition2();
        }

        @Override
        public String getTriggerItem3() {
            return emailTrigger.getTriggerCondition3();
        }

        @Override
        public String getTriggerItem4() {
            return emailTrigger.getTriggerCondition4();
        }

        @Override
        public String getTriggerItem5() {
            return emailTrigger.getTriggerCondition5();
        }
    }
}
