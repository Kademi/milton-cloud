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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.vfs.db.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class GroupEmailService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupEmailService.class);

    public void send(long jobId, Session session) {
        GroupEmailJob j = (GroupEmailJob) session.get(GroupEmailJob.class, jobId);
        if (j == null) {
            log.warn("Job not found: " + jobId);
        }
        log.info("send: " + j.getSubject() + " status: " + j.getStatus());
        if (j.readyToSend()) {
            generateEmailItems(j, session);
        } else {
            log.warn("Job is not ready to send. Current statyus: " + j.getStatus());
        }
    }

    /**
     * Generate EmailItem records to send. These will be sent by a seperate process
     * 
     * @param j
     * @param session 
     */
    private void generateEmailItems(GroupEmailJob j, Session session) {
        if (j.getGroupRecipients() == null) {
            log.warn("No recipients");
            return;
        }
        Set<Profile> profiles = new HashSet<>();
        for (GroupRecipient gr : j.getGroupRecipients()) {
            append(gr.getRecipient(), profiles);
        }
        log.info("recipients: " + profiles.size());
        Date now = _(CurrentDateService.class).getNow();
        for( Profile p : profiles ) {
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
            session.save(i);
            log.info("Created email item: " + i.getId() + " to " + p.getEmail());
        }
        j.setStatus("p");
        j.setStatusDate(now);
        session.save(j);
    }

    private void append(Group g, final Set<Profile> profiles) {
        log.info("append: group: " + g.getName());
        if (g.getMemberships() == null) {
            return;
        }
        
        VfsVisitor visitor = new AbstractVfsVisitor() {

            @Override
            public void visit(Group r) {
                append(r, profiles);
            }
            @Override
            public void visit(Profile p) {
                profiles.add(p);
            }            
        };
        
        for (GroupMembership m : g.getMemberships()) {
            System.out.println("membership: " + m.getMember().getName());
            m.getMember().accept(visitor); 
        }
    }
}
