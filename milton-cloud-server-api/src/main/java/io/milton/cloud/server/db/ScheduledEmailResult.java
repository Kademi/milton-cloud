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
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an execution of a scheduled email run
 *
 * @author brad
 */
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ScheduledEmailResult implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmailResult.class);
    
    public static ScheduledEmailResult findLatest(ScheduledEmail scheduledEmail, Session session) {
        Criteria crit = session.createCriteria(ScheduledEmailResult.class);
        crit.add(Restrictions.eq("scheduledEmail", scheduledEmail));
        crit.addOrder(Order.desc("startDate"));
        crit.setMaxResults(1);
        List<ScheduledEmailResult> results = DbUtils.toList(crit, ScheduledEmailResult.class);
        if( results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }    
    
    private long id;
    private ScheduledEmail scheduledEmail;
    private Date startDate;
    private Date completedDate;
    


    public ScheduledEmailResult() {
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
        
    
    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)         
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column()
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)         
    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date endDate) {
        this.completedDate = endDate;
    }

    @ManyToOne(optional = false)
    public ScheduledEmail getScheduledEmail() {
        return scheduledEmail;
    }

    public void setScheduledEmail(ScheduledEmail scheduledEmail) {
        this.scheduledEmail = scheduledEmail;
    }

    /**
     * Is expired if its not complete an hour after its start date
     * 
     * @param now
     * @return 
     */
    public boolean isExpired(Date now) {
        if( completedDate == null ) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(getStartDate());
        cal.add(Calendar.HOUR, 1);
        return now.after(cal.getTime());
    }


}
