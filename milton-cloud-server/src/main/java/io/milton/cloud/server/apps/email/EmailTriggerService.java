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
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.mail.BatchEmailService;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.event.TriggerEvent;
import io.milton.cloud.server.mail.EvaluationContext;
import io.milton.cloud.server.mail.FilterScriptEvaluator;
import io.milton.cloud.server.web.RootFolder;
import static io.milton.context.RequestContext._;
import org.hibernate.Session;

import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author brad
 */
public class EmailTriggerService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmailTriggerService.class);
    private final BatchEmailService batchEmailService;
    private final FilterScriptEvaluator filterScriptEvaluator;
    private final ApplicationManager applicationManager;

    public EmailTriggerService(BatchEmailService batchEmailService, FilterScriptEvaluator filterScriptEvaluator, ApplicationManager applicationManager) {
        this.batchEmailService = batchEmailService;
        this.filterScriptEvaluator = filterScriptEvaluator;
        this.applicationManager = applicationManager;
    }

    /**
     *
     * @param jobId
     * @param sourceEntities - id of the entity(s) which caused the trigger.
     * Usually the profile id of a user
     * @param session
     */
    public void send(long jobId, List<Long> sourceEntities, Session session) throws IOException {
        EmailTrigger j = (EmailTrigger) session.get(EmailTrigger.class, jobId);
        if (j == null) {
            log.warn("Job not found: " + jobId);
            return ;
        }

        List<BaseEntity> directRecips = new ArrayList<>();
        if (j.getGroupRecipients() != null && !j.getGroupRecipients().isEmpty() ) {
            for (GroupRecipient gr : j.getGroupRecipients()) {
                Group recipient = gr.getRecipient();
                if (recipient != null) {
                    addGroup(gr.getRecipient(), directRecips);
                } else {
                    log.warn("Couldnt find recipient for GroupRecipient=" + gr.getId());
                }
            }
        } else {
            log.info("No Group recipients specified for job: " + jobId);
        }

        List<Profile> sources = new ArrayList<>();
        for (Long entityId : sourceEntities) {
            Profile source = Profile.get(entityId, session);
            if (source != null) {
                sources.add(source);
            } else {
                log.warn("Couldnt find entity: " + entityId);
            }
        }


        if (j.isIncludeUser()) {
            for (Profile source : sources) {
                directRecips.add(source);
            }
        }

        Profile evalTarget = sources.get(0); // this is the user we will execute filter scripts against
        log.info("evalTarget: "+ evalTarget.getEmail());
        batchEmailService.generateEmailItems(j, evalTarget, directRecips, null, session);
        session.save(j);

    }

    private void addGroup(Group g, List<BaseEntity> recipients) {
        if (g.getGroupMemberships() != null) {
            for (GroupMembership gm : g.getGroupMemberships()) {
                recipients.add(gm.getMember());
            }
        }
    }

    public boolean checkConditions(EmailTrigger j, TriggerEvent event, Profile currentUser) {
        if( StringUtils.isBlank(j.getConditionScriptXml())) {
            return true;
        }
            RootFolder rf = null;
            if (j.getThemeSite() != null) {
                ApplicationManager appManager = _(ApplicationManager.class);
                rf = new WebsiteRootFolder(appManager, j.getThemeSite(), j.getThemeSite().getTrunk());
            } else {
                rf = new OrganisationRootFolder(applicationManager, j.getOrganisation());
            }        
        EvaluationContext evaluationContext = new EvaluationContext(j.getConditionScriptXml());
        evaluationContext.getAttributes().put("org", j.getOrganisation());        
        return filterScriptEvaluator.checkFilterScript(evaluationContext, currentUser, j.getOrganisation(), rf);
    }
}
