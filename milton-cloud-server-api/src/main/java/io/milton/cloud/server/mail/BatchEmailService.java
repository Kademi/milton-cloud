/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.mail;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.text.TextFromHtmlService;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.vfs.db.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.mvel2.templates.TemplateRuntime;

/**
 *
 * @author brad
 */
public class BatchEmailService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BatchEmailService.class);
    private final FilterScriptEvaluator filterScriptEvaluator;
    private final TextFromHtmlService textFromHtmlService = new TextFromHtmlService();
    private final ApplicationManager applicationManager;

    public BatchEmailService(FilterScriptEvaluator filterScriptEvaluator, ApplicationManager applicationManager) {
        this.filterScriptEvaluator = filterScriptEvaluator;
        this.applicationManager = applicationManager;
    }

    /**
     * Generate EmailItem records to send. These will be sent by a seperate
     * process
     *
     * @param j
     * @param directRecipients - list of users or groups. Groups will be
     * expanded to a list of users. The entire list of profiles is subject to
     * filtering
     * @param callback - may be null, otherwise is called just prior to
     * generating the email content
     * @param session
     * @throws IOException
     */
    public void generateEmailItems(BaseEmailJob j, List<BaseEntity> directRecipients, BatchEmailCallback callback, Session session) throws IOException {
        generateEmailItems(j, null, directRecipients, callback, session);
    }

    /**
     *
     * @param j
     * @param evaluationTarget - the user to be used when evaluating filters. If
     * null, will use the recipient
     * @param directRecipients - list of users or groups. Groups will be
     * expanded to a list of users. The entire list of profiles is subject to
     * filtering
     * @param callback
     * @param session
     * @throws IOException
     */
    public void generateEmailItems(BaseEmailJob j, Profile evaluationTarget, List<BaseEntity> directRecipients, BatchEmailCallback callback, Session session) throws IOException {
        Set<Profile> profiles = filterRecipients(j, evaluationTarget, directRecipients, session);
        if (profiles.isEmpty()) {
            log.warn("No recipients!! For job: " + j.getId());
            return;
        }

        log.info("recipients: " + profiles.size());
        if (j.getEmailItems() == null) {
            j.setEmailItems(new ArrayList<EmailItem>());
        }
        for (Profile p : profiles) {
            enqueueSingleEmail(j, p, callback, session);
        }
        session.save(j);
    }

    public Set<Profile> filterRecipients(BaseEmailJob j, Profile evaluationTarget, List<BaseEntity> directRecipients, Session session) {
        if (directRecipients.isEmpty()) {
            log.info("No direct recipients");
            return Collections.EMPTY_SET;
        }
        log.info("Direct recipients: " + directRecipients.size());
        Set<Profile> profiles = new HashSet<>();
        EvaluationContext evaluationContext = new EvaluationContext(j.getFilterScriptXml());
        evaluationContext.getAttributes().put("org", j.getOrganisation());        
        for (BaseEntity e : directRecipients) {
            if (e != null) {
                checkFilterAndAppend(j, evaluationTarget, e, evaluationContext, profiles);
            } else {
                log.warn("Found null recipient, ignoring");
            }
        }
        log.info("expanded recipients: " + profiles.size());
        return profiles;
    }

    /**
     * 
     * @param j
     * @param evaluationTarget - if not null is used as the subject to evaluation the filter, otherwise each recipient is used
     * @param g
     * @param evaluationContext
     * @param recipientList 
     */
    private void checkFilterAndAppend(final BaseEmailJob j, final Profile evaluationTarget,final BaseEntity g, final EvaluationContext evaluationContext, final Set<Profile> recipientList) {
        log.info("append: group: " + g.getId() + " - " + g.getId());

        final VfsVisitor visitor = new AbstractVfsVisitor() {
            @Override
            public void visit(Group r) {
                if (r.getGroupMemberships() != null) {
                    for (GroupMembership m : r.getGroupMemberships()) {
                        checkFilterAndAppend(j, evaluationTarget, m.getMember(), evaluationContext, recipientList);
                    }
                }
            }

            @Override
            public void visit(Profile p) {
                Profile target = evaluationTarget; // use the evaluationTarget if provided, otherwise the recipient
                if( target == null  ) {
                    target = p;
                }
                System.out.println("checkFilterAndAppend: " + target.getEmail());
                if (checkFilterScript(j, target, evaluationContext)) {
                    recipientList.add(p);
                }
            }
        };
        g.accept(visitor);
    }

    private boolean checkFilterScript(BaseEmailJob j, Profile evalTargetProfile, EvaluationContext evaluationContext) {
        if (j.getFilterScriptXml() != null && j.getFilterScriptXml().length() > 0) {
            RootFolder rf = null;
            if (j.getThemeSite() != null) {
                ApplicationManager appManager = _(ApplicationManager.class);
                rf = new WebsiteRootFolder(appManager, j.getThemeSite(), j.getThemeSite().getTrunk());
            } else {
                rf = new OrganisationRootFolder(applicationManager, j.getOrganisation());
            }
            return filterScriptEvaluator.checkFilterScript(evaluationContext, evalTargetProfile, j.getOrganisation(), rf);
        } else {
            return true;
        }
    }

    public void enqueueSingleEmail(BaseEmailJob j, Profile recipientProfile, BatchEmailCallback callback, Session session) throws HibernateException, IOException {
        String from = j.getFromAddress();
        if (from == null) {
            from = "@" + _(CurrentRootFolderService.class).getPrimaryDomain();
            if (j.getOrganisation().getAdminDomain() != null) {
                from = j.getOrganisation().getAdminDomain() + from;
            } else {
                from = "noreply" + from;
            }
        }

        String replyTo = from;
        Date now = _(CurrentDateService.class).getNow();
        EmailItem i = new EmailItem();
        i.setCreatedDate(now);
        i.setFromAddress(from);
        i.setReplyToAddress(replyTo);

        // Templating requires a HtmlPage to represent the template        
        i.setJob(j);
        i.setRecipient(recipientProfile);
        i.setRecipientAddress(recipientProfile.getEmail());
        i.setSendStatusDate(now);
        String subject = j.getSubject();
        if (subject == null || subject.trim().length() == 0) {
            subject = "Auto mail from " + j.getOrganisation().getFormattedName();
        }
        i.setSubject(subject);

        j.getEmailItems().add(i);
        session.save(i);
        String html = generateHtml(j, recipientProfile, callback, i);
        i.setHtml(html);
        String text = textFromHtmlService.generateTextFromHtml(html);
        i.setText(text);
        session.save(i);

        log.info("Created email item: " + i.getId() + " to " + recipientProfile.getEmail());
    }

    private String generateHtml(final BaseEmailJob j, final Profile p, BatchEmailCallback callback, EmailItem emailItem) throws IOException {
        return generateHtml(j.getThemeSite(), j.getHtml(), p, callback, emailItem);
    }

    public String generateHtml(Website themeSite, String html, final Profile p, BatchEmailCallback callback, EmailItem emailItem) throws IOException {
        Map localVars = new HashMap();
        localVars.put("profile", p);

        if (themeSite != null) {
            Branch b = themeSite.liveBranch();
            if (b != null) {
                WebsiteRootFolder websiteRootFolder = new WebsiteRootFolder(_(ApplicationManager.class), themeSite, b);
                localVars.put("website", websiteRootFolder);
            }
        }

        String template = html == null ? "" : html;
        if (callback != null) {
            template = callback.beforeSend(p, template, localVars, emailItem);
        }

        final String bodyHtml = TemplateRuntime.eval(template, localVars).toString();

        Map<String, String> params = new HashMap<>();

        if (themeSite != null) {
            Branch b = themeSite.liveBranch();
            if (b != null) {
                WebsiteRootFolder websiteRootFolder = new WebsiteRootFolder(_(ApplicationManager.class), themeSite, b);
                TemplatedHtmlPage page = new TemplatedHtmlPage("email", websiteRootFolder, "email/genericEmail", "Email") {
                    @Override
                    protected Map<String, Object> buildModel(Map<String, String> params) {
                        Map<String, Object> map = super.buildModel(params);
                        map.put("bodyHtml", bodyHtml);
                        return map;
                    }
                };

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try {
                    page.sendContent(bout, null, params, null);
                } catch (IOException | NotAuthorizedException | BadRequestException | NotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                return bout.toString("UTF-8");
            }
        }
        log.info(" no theme, cant do templating");
        return bodyHtml;
    }
}
