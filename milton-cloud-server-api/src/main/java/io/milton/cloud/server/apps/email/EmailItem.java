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
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.apps.forums.Forum;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

/**
 * Represents a task to send an email, which may have been sent or might
 * be queued to be sent.
 * 
 * Also represents an email received, if the recipient profile is set it will
 * show in their internal inbox.
 * 
 * Email attachments: TODO - probably just have a list of name and crc, then
 * the attachment can be put in any repository
 *
 * @author brad
 */
@Entity
public class EmailItem implements Serializable{
    
    public static List<EmailItem>  findByRecipient(Profile p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Expression.eq("recipient", p));
        crit.addOrder(Order.desc("sendStatusDate"));
        return DbUtils.toList(crit, EmailItem.class);
    }        
    
    private List<EmailSendAttempt> emailSendAttempts;

    private long id;
    private GroupEmailJob job; // optional, might be linked to a job
    private Profile sender; // optional, the user account which sent the email if originated internally
    private Profile recipient; // reference to user, possibly null
    private String recipientAddress; // actual email address being sent to
    private String fromAddress; // the stated from address
    private String replyToAddress; // reply-to field    
    
    private String subject; // subject line
    private String html; // body html, fully rendered. ie after applying template
    private String text; // body as plain text
    private Date createdDate;
    private String sendStatus; // status of the send job. c=completed, p=pending, r=retrying
    private Date sendStatusDate;
    
    private boolean readStatus; // status of reading the email, if delivered to an internal user.
    
    

    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne
    public GroupEmailJob getJob() {
        return job;
    }

    public void setJob(GroupEmailJob job) {
        this.job = job;
    }

    @ManyToOne
    public Profile getSender() {
        return sender;
    }

    public void setSender(Profile sender) {
        this.sender = sender;
    }

    @ManyToOne
    public Profile getRecipient() {
        return recipient;
    }

    public void setRecipient(Profile recipient) {
        this.recipient = recipient;
    }

    @Column(nullable=false)
    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    @Column(nullable=false)
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Column(nullable=false)
    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    @Column(nullable=false)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column(nullable=true)
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Column(nullable=true)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)           
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)           
    public Date getSendStatusDate() {
        return sendStatusDate;
    }

    public void setSendStatusDate(Date sendStatusDate) {
        this.sendStatusDate = sendStatusDate;
    }

    /**
     * Set to true once the item has been read
     * 
     * @return 
     */
    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    @OneToMany(mappedBy = "emailItem")
    public List<EmailSendAttempt> getEmailSendAttempts() {
        return emailSendAttempts;
    }

    public void setEmailSendAttempts(List<EmailSendAttempt> emailSendAttempts) {
        this.emailSendAttempts = emailSendAttempts;
    }

}
