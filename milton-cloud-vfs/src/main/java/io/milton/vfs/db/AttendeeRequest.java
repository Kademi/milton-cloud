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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a request for someone to attend a CalEvent, or the status of someone
 * who is attending
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
    
    public static final String PARTSTAT_TENTATIVE = "TENTATIVE";
    
    public static final String PARTSTAT_DECLINED = "DECLINED";
    
    public static final String PARTSTAT_NEEDS_ACTION = "NEEDS-ACTION";
    
    /**
     * Check if there is an attendee request for the given user on the given event.
     * If not create it
     * 
     * Sets the participation status to NEEDS-ACTION
     * 
     * @param attendeeProfile - the attendee profile
     * @param organisorsEvent - the organisor's event which the attendee may attend
     * @param now - the current date and time
     * @param scheduleAgent
     * @param session 
     * @return  
     */
    public static AttendeeRequest checkCreate(Profile attendeeProfile, CalEvent organisorsEvent, Date now, String scheduleAgent, Session session) {
        AttendeeRequest ar = findForUserAndEvent(attendeeProfile, organisorsEvent, session);
        if( ar != null ) {
            ar.setScheduleAgent(scheduleAgent);
            session.save(ar);
            return ar;
        }
        ar = new AttendeeRequest();        
        ar.setCreatedDate(now);
        if( attendeeProfile.getAttendeeRequests() == null ) {
            attendeeProfile.setAttendeeRequests(new ArrayList<>());
        }
        
        ar.setAttendee(attendeeProfile);
        ar.setName(UUID.randomUUID().toString());
        ar.setOrganiserEvent(organisorsEvent);
        ar.setParticipationStatus("NEEDS-ACTION");
        ar.setModifiedDate(now);
        ar.setScheduleAgent(scheduleAgent);
        attendeeProfile.getAttendeeRequests().add(ar);
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
    
    public static List<AttendeeRequest> findInvitationsForProfile(Profile invited, Organisation org, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        Criteria critOrganisorEvent = crit.createCriteria("organiserEvent");
        Criteria critOrganisorCalendar = critOrganisorEvent.createCriteria("calendar");
        critOrganisorCalendar.add(Restrictions.eq("baseEntity", org));
        
        crit.add(Restrictions.eq("attendee", invited));
        
        return DbUtils.toList(crit, AttendeeRequest.class);
    }        
    
    public static List<AttendeeRequest> findAcceptedByOrganisorEvent(CalEvent e, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("organiserEvent", e));
        crit.add(Restrictions.eq("participationStatus", PARTSTAT_ACCEPTED));
        return DbUtils.toList(crit, AttendeeRequest.class);
    }    
    
    public static AttendeeRequest findByName(String name, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("name", name));
        return DbUtils.unique(crit);
    }       
     
    
    public static List<AttendeeRequest> findByOrganisorEvent(CalEvent e, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("organiserEvent", e));
        return DbUtils.toList(crit, AttendeeRequest.class);
    }        

    public static Long countAttending(CalEvent event, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("organiserEvent", event));
        crit.add(Restrictions.eq("participationStatus", PARTSTAT_ACCEPTED));
        crit.setProjection(Projections.rowCount());
        return DbUtils.asLong(crit.list(), 0);
    }
    
    public static List<AttendeeRequest> findGuestsForProfileAndEvent(CalEvent organiserEvent, Profile member, Session session) {
        Criteria crit = session.createCriteria(AttendeeRequest.class);
        crit.add(Restrictions.eq("organiserEvent", organiserEvent));
        crit.add(Restrictions.eq("guestOf", member));
        return DbUtils.toList(crit, AttendeeRequest.class);        
    }

    public static AttendeeRequest get(Long id, Session session) {
        return (AttendeeRequest) session.get(AttendeeRequest.class, id);
    }
    
    private Long id;
    
    private String name; // the "file" name, globally unique
        
    private CalEvent organiserEvent;
    
    private CalEvent attendeeEvent;
        
    private Profile attendee; // if attendee is not null, contact fields below will be null
    
    private boolean acknowledged;
   
    private String participationStatus; // http://tools.ietf.org/html/draft-desruisseaux-caldav-sched-12#section-3.2.8
    
    private String firstName; // optional, only set if attendee is null
    
    private String surName; // optional, only set if attendee is null
    
    private String mail; // optional, only set if attendee is null
        
    private String orgName; // optional, only set if attendee is null
    
    private Profile guestOf; // will be set if this attendee is a guest
    
    private Date createdDate;
    
    private Date modifiedDate;
    
    private String scheduleAgent; // http://tools.ietf.org/html/rfc6638#section-7.1

    public AttendeeRequest() {
    }

    
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The resource name. Normally a UUID
     * 
     * @return 
     */
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
     * http://www.kanzaki.com/docs/ical/partstat.html
     * 
     * @return 
     */
    public String getParticipationStatus() {
        return participationStatus;
    }

    public void setParticipationStatus(String participationStatus) {
        this.participationStatus = participationStatus;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    @ManyToOne
    public Profile getGuestOf() {
        return guestOf;
    }

    public void setGuestOf(Profile guestOf) {
        this.guestOf = guestOf;
    }
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }    

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    
    
    /**
     * Usually "SERVER" or "CLIENT"
     * 
     * See http://tools.ietf.org/html/rfc6638#section-7.1
     * 
     * @return 
     */
    @Column(nullable = true)
    public String getScheduleAgent() {
        return scheduleAgent;
    }

    public void setScheduleAgent(String scheduleAgent) {
        this.scheduleAgent = scheduleAgent;
    }

    
    
    public void delete(Session session) {
        session.delete(this);
    }
    
    @Transient
    public String getAckStatus() {
        if( acknowledged ) {
            return "Acknowledged";
        } else {
            return "Not acked";
        }
    }
}
