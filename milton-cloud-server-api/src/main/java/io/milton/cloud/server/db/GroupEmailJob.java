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
import org.hibernate.criterion.Expression;

/**
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("G")
public class GroupEmailJob extends BaseEmailJob {

    /**
     * When set to READY_TO_SEND the job will be queued for sending by the dispatcher
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
        crit.add(Expression.eq("organisation", org));
        return DbUtils.toList(crit, GroupEmailJob.class);        
    }
    
    private String status;
    private Date statusDate;

    public GroupEmailJob() {
    }

    
    /**
     * c=completed
     * r=ready to send (ie as commanded by UI)
     * p=in progress (set by background process)
     * otherwise not started
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

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)       
    public Date getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }


    public boolean readyToSend() {
        return STATUS_READY_TO_SEND.equals(getStatus());
    }

    @Override
    public void accept(EmailJobVisitor visitor) {
        visitor.visit(this);
    }
}
