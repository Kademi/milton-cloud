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

import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Base class for entities which represent email jobs, such as a group email
 * or an email trigger
 * 
 * This base class is not itself has no status information because in this abstract
 * sense it can't be sent.
 *
 * @author brad
 */
@Entity
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 2)
@Inheritance(strategy = InheritanceType.JOINED)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class BaseEmailJob  implements Serializable{

    public abstract void accept(EmailJobVisitor visitor);
    
    private List<EmailItem> emailItems;
    private List<GroupRecipient> groupRecipients;
    private long id;
    private String type;
    private Organisation organisation;
    private String name;
    private String title;
    private String notes;
    private String subject;
    private String fromAddress;
    private String html;

    
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

    @Column
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Column
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @OneToMany(mappedBy = "job")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<GroupRecipient> getGroupRecipients() {
        return groupRecipients;
    }

    public void setGroupRecipients(List<GroupRecipient> groupRecipients) {
        this.groupRecipients = groupRecipients;
    }

    @OneToMany(mappedBy = "job")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<EmailItem> getEmailItems() {
        return emailItems;
    }

    public void setEmailItems(List<EmailItem> emailItems) {
        this.emailItems = emailItems;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    
    /**
     * Adds, but does not save, the group recipient
     */
    public void addGroupRecipient(Group g) {
        if( this.getGroupRecipients() == null ) {
            setGroupRecipients(new ArrayList<GroupRecipient>());
        }
        GroupRecipient gr = new GroupRecipient();
        gr.setJob(this);
        gr.setRecipient(g);
        getGroupRecipients().add(gr);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    
}
