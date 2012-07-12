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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.mail.MiltonCloudMailResourceFactory;
import io.milton.cloud.server.web.*;
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
import io.milton.mail.StandardMessageFactory;
import io.milton.mail.StandardMessageFactoryImpl;
import io.milton.vfs.db.utils.SessionManager;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.listener.ListenerManager;

/**
 *
 * @author brad
 */
public class EmailApp implements MenuApplication, LifecycleApplication, PortletApplication {

    private MiltonCloudMailResourceFactory mailResourceFactory;
    private MailServer mailServer;
    private SpliffySecurityManager securityManager;
    private GroupEmailService groupEmailService;
    private Configuration aspirinConfiguration = new Configuration();
    private ListenerManager listenerManager = new ListenerManager();
    private EmailItemQueueStore queueStore;
    private EmailItemMailStore mailStore;
    private CurrentDateService currentDateService;

    @Override
    public String getInstanceId() {
        return "email";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        groupEmailService = new GroupEmailService();
        securityManager = resourceFactory.getSecurityManager();
        mailResourceFactory = new MiltonCloudMailResourceFactory();
        this.currentDateService = config.getContext().get(CurrentDateService.class);
        queueStore = new EmailItemQueueStore(resourceFactory.getSessionManager(), aspirinConfiguration, listenerManager, currentDateService);
        StandardMessageFactory smf = new StandardMessageFactoryImpl();
        mailStore = new EmailItemMailStore(resourceFactory.getSessionManager(), smf);

        MailServerBuilder mailServerBuilder = new MailServerBuilder();
        mailServerBuilder.setListenerManager(listenerManager);
        mailServerBuilder.setAspirinConfiguration(aspirinConfiguration);
        mailServerBuilder.setMailResourceFactory(mailResourceFactory);
        mailServerBuilder.setEnablePop(false);
        mailServerBuilder.setEnableMsa(false);
        mailServerBuilder.setSmtpPort(2525); // high port for linux. TODO: make configurable        
        mailServerBuilder.setMailStore(mailStore);
        mailServerBuilder.setQueueStore(queueStore);
        mailServer = mailServerBuilder.build();
        mailStore.setAspirinInternal(mailServerBuilder.getAspirinInternal());
        mailServer.start();
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof GroupEmailAdminFolder) {
            GroupEmailAdminFolder faf = (GroupEmailAdminFolder) parent;
            if (requestedName.equals("manage")) {
                MenuItem.setActiveIds("menuTalk", "menuEmails", "menuSendEmail");
                return new ManageGroupEmailsPage(requestedName, faf.getOrganisation(), faf);
            }
        }
        if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            if (requestedName.equals("inbox")) {
                return new MyInboxPage(requestedName, wrf);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder orgFolder = (OrganisationFolder) parent;
            children.add(new GroupEmailAdminFolder("groupEmails", orgFolder, orgFolder.getOrganisation(), groupEmailService));
        }
        if (parent instanceof UserResource) {
            UserResource ur = (UserResource) parent;
            EmailFolder f = new EmailFolder(ur, "inbox");
            children.add(f);
        }
    }

    @Override
    public void appendMenu(MenuItem parent) {
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
                    parent.getOrCreate("menuSendEmail", "Send and manage emails", parentOrg.getPath().child("groupEmails").child("manage")).setOrdering(10);
                }
                break;
        }
    }

    private String getEmailMenuItemText() {
        Profile currentUser = securityManager.getCurrentUser();
        int numUnread = EmailItem.findByNumUnreadByRecipient(currentUser, SessionManager.session());
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
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (portletSection.equals("messages")) {
            _(TextTemplater.class).writePage("email/emailMessagesPortlet.html", currentUser, rootFolder, context, writer);
        }
//            <div>                
//                <div class="txtR">
//                    <h3>Hi Patrick, Welcome to your dashboard!</h3>
//                    <p>Below you will find a list of your most recent and active modules.  A panel which gives you an overview of your eLearning progress and a snapshot of what people are talking about in the community.  Happy learning.</p>
//                </div>
//                <div class="clr"></div>
//                <a class="close" href="#">close</a>
//            </div>

    }
}
