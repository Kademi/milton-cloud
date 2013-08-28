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
import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.db.PasswordReset;
import java.util.Date;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class GroupEmailService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupEmailService.class);
    private final BatchEmailService batchEmailService;

    public GroupEmailService(BatchEmailService batchEmailService) {
        this.batchEmailService = batchEmailService;
    }

    public void send(long jobId, Session session) throws IOException {
        GroupEmailJob j = (GroupEmailJob) session.get(GroupEmailJob.class, jobId);
        if (j == null) {
            log.warn("Job not found: " + jobId);
            return ;
        }
        log.info("send: " + j.getSubject() + " status: " + j.getStatus());
        if (j.readyToSend()) {
            Transaction tx = session.beginTransaction();
            generateEmailItems(j, session);
            tx.commit();
        } else {
            log.warn("Job is not ready to send. Current statyus: " + j.getStatus());
        }
    }

    public void sendPreview(GroupEmailJob j, Profile recipientProfile, Session session) throws HibernateException, IOException {
        if (StringUtils.isNotBlank(j.getPasswordResetLinkText())) {
            if (j.getThemeSite() == null) {
                throw new RuntimeException("Cant send password reset group email because no theme has been selected");
            }
        }

        BatchEmailCallback callback = getCallback(j, session);
        batchEmailService.enqueueSingleEmail(j, recipientProfile,true, callback, session);
    }

    /**
     * Generate EmailItem records to send. These will be sent by a seperate
     * process
     *
     * @param j
     * @param session
     */
    private void generateEmailItems(GroupEmailJob j, Session session) throws IOException {
        boolean isPasswordReset = j.isPasswordReset() == null ? false : j.isPasswordReset();
        if (j.getThemeSite() == null && isPasswordReset) {
            throw new RuntimeException("Cant send password reset group email because no theme has been selected");
        }
        List<BaseEntity> directRecips = getRecipients(j);
        if( directRecips.isEmpty()) {
            throw new RuntimeException("No recipients");
        }

        final Date now = _(CurrentDateService.class).getNow();
        BatchEmailCallback callback = getCallback(j, session);

        j = (GroupEmailJob) batchEmailService.generateEmailItems(j, directRecips, callback, session);

        if (directRecips.isEmpty()) {
            j.setStatus(GroupEmailJob.STATUS_COMPLETED);
        } else {
            j.setStatus(GroupEmailJob.STATUS_IN_PROGRESS);
        }
        j.setStatusDate(now);
        session.save(j);
    }

    public List<BaseEntity> getRecipients(final GroupEmailJob j) {
        boolean isPasswordReset = j.isPasswordReset() == null ? false : j.isPasswordReset();
        List<BaseEntity> directRecips = new ArrayList<>();
        if (j.getGroupRecipients() != null && !j.getGroupRecipients().isEmpty()) {
            for (GroupRecipient gr : j.getGroupRecipients()) {
                addGroup(gr.getRecipient(), directRecips, isPasswordReset);
            }
        } else {
            log.warn("No group recipients for job: " + j.getId());
        }
        return directRecips;
    }

    private void addGroup(Group g, List<BaseEntity> recipients, boolean isPasswordReset) {
        if (g.getGroupMemberships() != null && !g.getGroupMemberships().isEmpty()) {
            for (GroupMembership gm : g.getGroupMemberships()) {
                Profile p = gm.getMember();
                // if its a password reset, only send it to those accounts which do not have a password
                // thats because this is used to welcome user's who have been loaded into the system but who need to create a password
                if (!isPasswordReset || p.getPasswordCredentialDate() == null) {
                    // if there is a script execute it to check if to send to this user

                    recipients.add(p);
                }
            }
        } else {
            log.warn("No members in recipient group: " + g.getName());
        }
    }

    private BatchEmailCallback getCallback(final GroupEmailJob j, final Session session) {
        BatchEmailCallback callback = null;
        if (j.isPasswordReset() != null && j.isPasswordReset()) {
            callback = new BatchEmailCallback() {
                @Override
                public String beforeSend(Profile p, String template, Map templateVars, EmailItem emailItem) {
                    Website website = j.getThemeSite();
                    String returnUrl = UserApp.getPasswordResetBase(website);
                    final Date now = _(CurrentDateService.class).getNow();
                    PasswordReset passwordReset = PasswordReset.create(p, now, returnUrl, website, session);
                    templateVars.put("passwordReset", passwordReset);
                    String resetHref = returnUrl + "?token=" + passwordReset.getToken();
                    templateVars.put("resetHref", resetHref);
                    String linkText = j.getPasswordResetLinkText() == null ? "Please click here to reset your password" : j.getPasswordResetLinkText();
                    String passwordResetLinkTemplate = "<a href='" + resetHref + "'>" + linkText + "</a>";
                    return template + passwordResetLinkTemplate;
                }
            };
        }
        return callback;
    }
}
