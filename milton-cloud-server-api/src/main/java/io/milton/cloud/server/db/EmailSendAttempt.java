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

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.Session;

/**
 * Represents an attempt of sending an email, which might have been successful 
 * or not.
 *
 * @author brad
 */
@Entity
public class EmailSendAttempt implements Serializable{

    private long id;
    private EmailItem emailItem;
    private String status; // status of the send job. c=completed, p=pending, f=failed
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
    public EmailItem getEmailItem() {
        return emailItem;
    }

    public void setEmailItem(EmailItem emailItem) {
        this.emailItem = emailItem;
    }

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

    public void delete(Session session) {
        session.delete(this);
    }

    
}
