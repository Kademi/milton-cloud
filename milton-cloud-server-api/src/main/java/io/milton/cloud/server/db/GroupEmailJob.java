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
public class GroupEmailJob implements Serializable{

    public static List<GroupEmailJob> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(GroupEmailJob.class);
        crit.add(Expression.eq("organisation", org));
        return DbUtils.toList(crit, GroupEmailJob.class);        
    }
    
    private List<EmailItem> emailItems;
    private List<GroupRecipient> groupRecipients;
    private long id;
    private Organisation organisation;
    private String name;
    private String title;
    private String notes;
    private String subject;
    private String fromAddress;
    private String status;
    private Date statusDate;

    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable=false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(nullable=true)
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Column(nullable=false)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column(nullable=false)
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @OneToMany(mappedBy = "job")
    public List<GroupRecipient> getGroupRecipients() {
        return groupRecipients;
    }

    public void setGroupRecipients(List<GroupRecipient> groupRecipients) {
        this.groupRecipients = groupRecipients;
    }

    /**
     * c=completed
     * p=in progress
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

    @OneToMany(mappedBy = "job")
    public List<EmailItem> getEmailItems() {
        return emailItems;
    }

    public void setEmailItems(List<EmailItem> emailItems) {
        this.emailItems = emailItems;
    }
    
    
    
    
    
}
