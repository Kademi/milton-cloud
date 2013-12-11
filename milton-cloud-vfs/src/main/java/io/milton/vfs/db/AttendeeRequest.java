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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a request for someone to attend a CalEvent
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(
        uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name"})})
public class AttendeeRequest implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(AttendeeRequest.class);
    
    public static final String PARTSTAT_ACCEPTED = "ACCEPTED";
    
    public static final String PARTSTAT_DECLINED = "DECLINED";
    
    public static final String PARTSTAT_NEEDS_ACTION = "NEEDS-ACTION";
    
    /**
     * Check if there is an attendee request for the given user on the given event.
     * If not create it
     * 
     * @param p
     * @param e
     * @param session 
     * @return  
     */
    public static AttendeeRequest checkCreate(Profile p, CalEvent e, Session session) {
        AttendeeRequest ar = findForUserAndEvent(p, e, session);
        if( ar != null ) {
            return ar;
        }
        ar = new AttendeeRequest();        
        if( p.getAttendeeRequests() == null ) {
            p.setAttendeeRequests(new ArrayList<AttendeeRequest>());
        }
        
        ar.setAttendee(p);
        ar.setName(UUID.randomUUID().toString());
        ar.setOrganiserEvent(e);
        ar.setParticipationStatus("NEEDS-ACTION");
        p.getAttendeeRequests().add(ar);
        session.save(ar);
        log.info("Created AttendeeRequest: " + ar.getId());
        return ar;
    }
    
    public static AttendeeRequest findForUserAndEvent(Profile p, CalEvent e, Session session) {
        if( p.getAttendeeRequests() == null ) {
            return null;
        }
        for( AttendeeRequest ar : p.getAttendeeRequests() ) {
            if( ar.getOrganiserEvent().getId().equals(e.getId()) ) {
                // exists, so abort
                return ar;
            }
        }
        return null;
    }
    
    public static List<AttendeeRequest> findByOrganisorEvent(CalEvent e, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("organiserEvent", e));
        crit.add(Restrictions.eq("participationStatus", PARTSTAT_ACCEPTED));
        return DbUtils.toList(crit, AttendeeRequest.class);
    }    
    
    private Long id;
    
    private String name; // the "file" name, globally unique
        
    private CalEvent organiserEvent;
    
    private CalEvent attendeeEvent;
    
    private Profile attendee;
    
    private boolean acknowledged;
   
    private String participationStatus; // http://tools.ietf.org/html/draft-desruisseaux-caldav-sched-12#section-3.2.8
    
    
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

    @ManyToOne
    public CalEvent getOrganiserEvent() {
        return organiserEvent;
    }

    public void setOrganiserEvent(CalEvent organiserEvent) {
        this.organiserEvent = organiserEvent;
    }

    @OneToOne
    public CalEvent getAttendeeEvent() {
        return attendeeEvent;
    }

    public void setAttendeeEvent(CalEvent attendeeEvent) {
        this.attendeeEvent = attendeeEvent;
    }

    @ManyToOne
    public Profile getAttendee() {
        return attendee;
    }

    public void setAttendee(Profile attendee) {
        this.attendee = attendee;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    /**
     * 
     * 
     * @return 
     */
    public String getParticipationStatus() {
        return participationStatus;
    }

    public void setParticipationStatus(String participationStatus) {
        this.participationStatus = participationStatus;
    }


    
}
