/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import io.milton.vfs.db.Group;
import io.milton.vfs.db.Profile;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents an explicit action by a user to "opt-in" to receive a specific
 * category of communications.
 * 
 * Such an action can be: 
 * 
 * 1. registering (so communications relating
 * to that membership are clearly intended)
 * 
 * 2. select a checkbox asking to opt-in
 *
 * @author brad
 */
@javax.persistence.Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OptInLog {
    
    /**
     * User has selected to opt-in via an option on registration
     */
    public static String SELECT_OPTION = "SELECT OPTIN OPTION";
    
    /**
     * User opted in by registering
     */
    public static String REGISTERED = "REGISTERED";
    
    
    
    public static void create(Profile p, String sourceIp, Group optinGroup, String optinAction, Session session) {
        OptInLog o = new OptInLog();
        o.setCreatedDate(new Date());
        o.setOptinAction(optinAction);
        o.setOptinGroup(optinGroup);
        o.setProfile(p);
        o.setSourceIp(sourceIp);
        session.save(o);
    }
    
    private long id;
    private Profile profile; // who opted in
    private Date createdDate; // when they did it
    private String sourceIp; // the IP address of the user's browser when they opted in
    private Group group; // the group they've opted in to
    private String optinAction; // the action which indicates acceptance. May be message of checkbox, or explicit registration

    @Id
    @GeneratedValue        
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Column(nullable=true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)       
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    @ManyToOne(optional=false)
    public Group getOptinGroup() {
        return group;
    }

    public void setOptinGroup(Group group) {
        this.group = group;
    }

    @Column(nullable=false)
    public String getOptinAction() {
        return optinAction;
    }

    public void setOptinAction(String optinAction) {
        this.optinAction = optinAction;
    }
}
