/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.scheduler;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.reporting.ReportingApp;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.db.ScheduledEmail;
import io.milton.cloud.server.db.ScheduledEmailResult;
import io.milton.cloud.server.mail.BatchEmailCallback;
import io.milton.cloud.server.mail.BatchEmailService;
import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.BufferingOutputStream;
import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;
import org.hibernate.Session;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SchedulerApp implements Application, MenuApplication, ChildPageApplication {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SchedulerApp.class);
    private SchedulerProcessor schedulerProcessor;
    private int pollScheduledTasks = 1000 * 60 * 60;

    @Override
    public String getInstanceId() {
        return "scheduler";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        schedulerProcessor = new SchedulerProcessor(this, resourceFactory.getApplicationManager());
        AsynchProcessor asynchProcessor = config.getContext().get(AsynchProcessor.class);
        asynchProcessor.schedule(schedulerProcessor, pollScheduledTasks);
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Scheduler app";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Runs tasks at specified times and intervals";
    }

    @Override
    public void appendMenu(MenuItem parent) {
        switch (parent.getId()) {
            case "menuRoot":
                if (parent.getRootFolder() instanceof OrganisationRootFolder) {
                    parent.getOrCreate("menuTalk", "Talk &amp; Connect").setOrdering(30);
                }
                break;
            case "menuTalk":
                parent.getOrCreate("menuScheduler", "Scheduled tasks").setOrdering(30);
                break;
            case "menuScheduler":
                OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
                if (parentOrg != null) {
                    parent.getOrCreate("menuSchedulerEmail", "Scheduled emails", parentOrg.getPath().child("scheduledEmails")).setOrdering(10);
                }
                break;
        }
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationRootFolder) {
            OrganisationRootFolder rf = (OrganisationRootFolder) parent;
            if (requestedName.equals("scheduledEmails")) {
                return new ManageScheduledEmailsFolder(requestedName, rf);
            }
        }
        return null;
    }

    public void sendScheduledEmail(final ScheduledEmail due, final Date fromDate, final Date toDate, final Session session) throws IOException {
        BatchEmailService batchEmailService = _(BatchEmailService.class);
        List<BaseEntity> directRecips = getRecipients(due);
        session.save(due);
        batchEmailService.generateEmailItems(due, directRecips, new BatchEmailCallback() {
            @Override
            public String beforeSend(Profile p, String template, Map templateVars, EmailItem emailItem) {
                // process attachment
                templateVars.put("fromDate", fromDate);
                templateVars.put("toDate", toDate);
                String dateRange = ReportingApp.getDateRange(fromDate, toDate);
                templateVars.put("dateRange", dateRange);

                ApplicationManager applicationManager = _(ApplicationManager.class);
                OrganisationRootFolder orf = new OrganisationRootFolder(applicationManager, due.getOrganisation());
                try {
                    Resource r = orf.find(due.getHrefTemplate());
                    if( r instanceof GetableResource) {
                        GetableResource gr = (GetableResource) r;
                        BufferingOutputStream bout = new BufferingOutputStream(50000);
                        gr.sendContent(bout, null, templateVars, null);
                        bout.close();
                        
                        BlobStore blobStore = _(BlobStore.class);
                        HashStore hashStore = _(HashStore.class);
                        Parser parser = new Parser();
                        String fileHash;
                        try( InputStream bin = bout.getInputStream() ) {
                            fileHash = parser.parse(bin, hashStore, blobStore);
                        }
                        session.save(emailItem); // must be saved before add attachment
                        emailItem.addAttachment(gr.getName(), fileHash, gr.getContentType(null), null, session);
                    }
                } catch (NotAuthorizedException | BadRequestException | IOException | NotFoundException e ) {
                    throw new RuntimeException(e);
                }

                if (due.getHrefTemplate() != null && due.getHrefTemplate().trim().length() > 0) {
                    final String href = TemplateRuntime.eval(template, templateVars).toString();
                    if (due.isAttachHref()) {
                        addAttachment(href, emailItem);
                    } else {
                        template += "<p><a href='" + href + "'>Click here to view the attachment</a></p>";
                    }
                }

                return template;
            }
        }, session);
    }

    private void addAttachment(String href, EmailItem emailItem) {
    }

    public List<BaseEntity> getRecipients(final BaseEmailJob j) {
        List<BaseEntity> directRecips = new ArrayList<>();
        if (j.getGroupRecipients() != null && !j.getGroupRecipients().isEmpty()) {
            for (GroupRecipient gr : j.getGroupRecipients()) {
                addGroup(gr.getRecipient(), directRecips);
            }
        } else {
            log.warn("No group recipients for job: " + j.getId());
        }
        return directRecips;
    }

    private void addGroup(Group g, List<BaseEntity> recipients) {
        if (g.getGroupMemberships() != null && !g.getGroupMemberships().isEmpty()) {
            for (GroupMembership gm : g.getGroupMemberships()) {
                recipients.add(gm.getMember());
            }
        } else {
            log.warn("No members in recipient group: " + g.getName());
        }
    }
}
