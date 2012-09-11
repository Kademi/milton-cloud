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

import io.milton.cloud.server.mail.BatchEmailService;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupRecipient;
import org.hibernate.Session;

import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class EmailTriggerService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmailTriggerService.class);
    private final BatchEmailService batchEmailService;

    public EmailTriggerService(BatchEmailService batchEmailService) {
        this.batchEmailService = batchEmailService;
    }

    /**
     *
     * @param jobId
     * @param sourceEntities - id of the entity(s) which caused the trigger.
     * Usually the profile id of a user
     * @param session
     */
    public void send(long jobId, List<Long> sourceEntities, Session session) {
        EmailTrigger j = (EmailTrigger) session.get(EmailTrigger.class, jobId);
        if (j == null) {
            log.warn("Job not found: " + jobId);
        }

        List<BaseEntity> directRecips = new ArrayList<>();
        if (j.getGroupRecipients() != null) {
            for (GroupRecipient gr : j.getGroupRecipients()) {
                addGroup(gr.getRecipient(), directRecips);
            }
        }
        
        if( j.isIncludeUser()) {
            for( Long entityId : sourceEntities ) {
                BaseEntity source = (BaseEntity) session.get(BaseEntity.class, entityId);
                directRecips.add(source);
            }
        }
        
        batchEmailService.generateEmailItems(j, directRecips, session);
        session.save(j);

    }
    
    private void addGroup(Group g, List<BaseEntity> recipients) {
        if( g.getGroupMemberships() != null ) {
            for( GroupMembership gm : g.getGroupMemberships() ) {
                recipients.add(gm.getMember());
            }
        }
    }
}
