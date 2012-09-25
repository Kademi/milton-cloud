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
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.vfs.db.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class BatchEmailService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BatchEmailService.class);

    /**
     * Generate EmailItem records to send. These will be sent by a seperate
     * process
     *
     * @param j
     * @param directRecipients- recipients directly referenced by the job. This will be expanded to profiles
     * @param session
     */
    public void generateEmailItems(BaseEmailJob j, List<BaseEntity> directRecipients, Session session) {
        if (j.getGroupRecipients() == null && directRecipients.isEmpty()) {
            log.warn("No recipients!! For job: " + j.getId());
            return;
        }
        Set<Profile> profiles = new HashSet<>();
        for (BaseEntity e : directRecipients) {
            append(e, profiles);
        }
        log.info("recipients: " + profiles.size());
        Date now = _(CurrentDateService.class).getNow();
        if( j.getEmailItems() == null ) {
            j.setEmailItems(new ArrayList<EmailItem>());
        }
        for (Profile p : profiles) {
            EmailItem i = new EmailItem();
            i.setCreatedDate(now);
            i.setFromAddress(j.getFromAddress());
            i.setHtml(j.getHtml()); // todo: templating
            i.setJob(j);
            i.setRecipient(p);
            i.setRecipientAddress(p.getEmail());
            i.setReplyToAddress(j.getFromAddress()); // todo: make this something more robust in terms of SPF?
            i.setSendStatusDate(now);
            i.setSubject(j.getSubject());
            j.getEmailItems().add(i);
            session.save(i);
            log.info("Created email item: " + i.getId() + " to " + p.getEmail());
        }
        session.save(j);
    }

    private void append(BaseEntity g, final Set<Profile> profiles) {
        log.info("append: group: " + g.getName() + " - " + g.getId());

        final VfsVisitor visitor = new AbstractVfsVisitor() {

            @Override
            public void visit(Group r) {
                if (r.getGroupMemberships() != null) {
                    for (GroupMembership m : r.getGroupMemberships()) {
                        append(m.getMember(), profiles);
                    }
                }
            }

            @Override
            public void visit(Profile p) {
                profiles.add(p);
            }
        };
        g.accept(visitor);
    }
}
