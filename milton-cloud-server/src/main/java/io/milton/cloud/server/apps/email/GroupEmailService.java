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
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import java.util.Date;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import java.util.ArrayList;
import java.util.List;
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
    
    
    
    public void send(long jobId, Session session) {
        GroupEmailJob j = (GroupEmailJob) session.get(GroupEmailJob.class, jobId);
        if (j == null) {
            log.warn("Job not found: " + jobId);
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

    /**
     * Generate EmailItem records to send. These will be sent by a seperate process
     * 
     * @param j
     * @param session 
     */
    private void generateEmailItems(GroupEmailJob j, Session session) {
        List<BaseEntity> directRecips = new ArrayList<>();
        if (j.getGroupRecipients() != null && !j.getGroupRecipients().isEmpty() ) {
            for (GroupRecipient gr : j.getGroupRecipients()) {
                addGroup(gr.getRecipient(), directRecips);
            }
        } else {
            log.warn("No group recipients for job: " + j.getId());
        }  
        batchEmailService.generateEmailItems(j, directRecips, session);
        
        Date now = _(CurrentDateService.class).getNow();
        j.setStatus("p");
        j.setStatusDate(now);
        session.save(j);
        
    }
    
    private void addGroup(Group g, List<BaseEntity> recipients) {
        if( g.getGroupMemberships() != null && !g.getGroupMemberships().isEmpty() ) {
            for( GroupMembership gm : g.getGroupMemberships() ) {
                recipients.add(gm.getMember());
            }
        } else {
            log.warn("No members in recipient group: " + g.getName());
        }
    }    
}
