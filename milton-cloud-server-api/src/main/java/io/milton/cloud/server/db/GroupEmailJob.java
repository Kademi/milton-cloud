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
package io.milton.cloud.server.db;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.DbUtils;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("G")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupEmailJob extends BaseEmailJob {

    private static final Logger log = LoggerFactory.getLogger(GroupEmailJob.class);
    
    /**
     * When set to READY_TO_SEND the job will be queued for sending by the
     * dispatcher
     */
    public static final String STATUS_READY_TO_SEND = "r";
    /**
     * This status indicates that the email dispatcher is processing the job
     */
    public static final String STATUS_IN_PROGRESS = "p";
    /**
     * Completed: the dispatcher has completed sending the job
     */
    public static final String STATUS_COMPLETED = "c";

    public static List<GroupEmailJob> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(GroupEmailJob.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", org));
        return DbUtils.toList(crit, GroupEmailJob.class);
    }
    
    public static List<GroupEmailJob> findInProgress(Session session) {
        Criteria crit = session.createCriteria(GroupEmailJob.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("status", STATUS_IN_PROGRESS));
        return DbUtils.toList(crit, GroupEmailJob.class);
    }    
    
    private String status;
    private Date statusDate;
    private Boolean passwordReset;
    private String passwordResetLinkText;
    

    public GroupEmailJob() {
    }

    /**
     * c=completed r=ready to send (ie as commanded by UI) p=in progress (set by
     * background process) otherwise not started
     *
     * @return
     */
    @Column
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }
    
    /**
     * If true, sending this email will generate a password reset token for each user
     * 
     * @return 
     */
    @Column
    public Boolean isPasswordReset() {
        return passwordReset;
    }
    
    public void setPasswordReset(Boolean b) {
        this.passwordReset = b;
    }

    @Column
    public String getPasswordResetLinkText() {
        return passwordResetLinkText;
    }

    public void setPasswordResetLinkText(String passwordResetLinkText) {
        this.passwordResetLinkText = passwordResetLinkText;
    }

    
    
    public boolean readyToSend() {
        return STATUS_READY_TO_SEND.equals(getStatus());
    }

    public boolean completed() {
        return STATUS_COMPLETED.equals(getStatus());
    }
    
    public boolean inProgress() {
        return STATUS_IN_PROGRESS.equals(getStatus());
    }    
    
    
    @Override
    public void accept(EmailJobVisitor visitor) {
        visitor.visit(this);
    }

    public void checkStatus(Date now, Session session) {        
        if( !inProgress() ) {
            log.info("checkStatus: not in progress");
            return ;
        }
        // If all email items are completed or failed then the job is complete
        if( getEmailItems() != null ) {
            for( EmailItem i : getEmailItems() ) {
                boolean finished = i.complete() || i.failed();
                if( !finished ) {
                    log.info("checkStatus: found an unfinished item");
                    return ; // not finished
                }
            }
        }
        log.info("checkStatus: All items are finished, so mark job as complete");
        setStatus(GroupEmailJob.STATUS_COMPLETED);
        setStatusDate(now);
        session.save(this);

    }

    
}
