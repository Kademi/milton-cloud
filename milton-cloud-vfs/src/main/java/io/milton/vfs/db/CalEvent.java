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
package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CalEvent implements Serializable {
    
    public static List<CalEvent> find(Calendar cal, Date from, Date to, Session session) {
        Criteria crit = session.createCriteria(CalEvent.class);
        crit.add( Restrictions.eq("calendar", cal));
        if( from != null ) {
            crit.add(Restrictions.ge("startDate", from));
        }
        if( to != null ) {
            crit.add(Restrictions.le("startDate", to));
        }
        
        crit.addOrder(Order.asc("startDate"));
        return DbUtils.toList(crit, CalEvent.class);
    }    

    public static List<CalEvent> find(BaseEntity owner, Date from, Date to, Session session) {
        Criteria crit = session.createCriteria(CalEvent.class);
        Criteria critCal = crit.createCriteria("calendar");
        critCal.add( Restrictions.eq("baseEntity", owner));
        if( from != null ) {
            crit.add(Restrictions.ge("startDate", from));
        }
        if( to != null ) {
            crit.add(Restrictions.le("startDate", to));
        }
        
        crit.addOrder(Order.asc("startDate"));
        return DbUtils.toList(crit, CalEvent.class);
    }    
    
    
    private AttendeeRequest attendeeRequest;
    
    private Long id;
    
    private String name; // the "file" name
        
    private Calendar calendar;
    
    private Date createdDate;
    
    private Date modifiedDate;
    
    private Date startDate;
    
    private Date endDate;
    
    private String timezone;
    
    private String summary;
    
    private String description;
    
    
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(optional=false)
    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }
    
    
    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the startDate
     */
    @Column(nullable=false)    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the endDate
     */
    @Column(nullable=false)    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the timezone
     */
    @Column
    public String getTimezone() {
        return timezone;
    }

    /**
     * @param timezone the timezone to set
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * @return the summary
     */
    @Column
    public String getSummary() {
        return summary;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * @return the description
     */
    @Column
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @OneToOne(mappedBy = "attendeeEvent")
    public AttendeeRequest getAttendeeRequest() {
        return attendeeRequest;
    }

    public void setAttendeeRequest(AttendeeRequest attendeeRequest) {
        this.attendeeRequest = attendeeRequest;
    }
}
