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
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a task to send an email, which may have been sent or might be
 * queued to be sent.
 *
 * Also represents an email received, if the recipient profile is set it will
 * show in their internal inbox.
 *
 * Email attachments: TODO - probably just have a list of name and crc, then the
 * attachment can be put in any repository
 *
 * @author brad
 */
@Entity
public class EmailItem implements Serializable {
    
    private static final Logger log = LoggerFactory.getLogger(EmailItem.class);

    /**
     * Get emauils to send. TODO: currently only gets those never tried, should
     * also get those queued for retry
     *
     * @param session
     * @return
     */
    public static List<EmailItem> findToSend(Date now, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        // sendStatus must be null or "r" = try
        crit.add(Restrictions.or(
                Restrictions.isNull("sendStatus"),
                Restrictions.eq("sendStatus", "r")));
        // and nextAttempt date must be null or past
//        crit.add(Expression.or(
//                Expression.isNull("nextAttempt"),
//                Expression.gt("nextAttempt", now)
//        ));
        crit.addOrder(Order.asc("createdDate"));
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static List<EmailItem> findByRecipient(BaseEntity p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.eq("recipient", p));
        crit.addOrder(Order.desc("sendStatusDate"));
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static long findByNumUnreadByRecipient(Profile p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.eq("recipient", p));
        crit.add(Restrictions.eq("readStatus", false));
        crit.setProjection(Projections.rowCount());
        List results = crit.list();
        if (results == null) {
            return 0;
        }
        Object o = results.get(0);
        if (o instanceof Long) {
            Long num = (Long) o;
            return num;
        } else if (o instanceof Integer) {
            Integer ii = (Integer) o;
            return ii.intValue();
        } else {
            if (o != null) {
                log.error("Unsupported value type: " + o.getClass());
            }
            return 0;
        }
    }

    public static List<EmailItem> findInProgress(Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        // sendStatus must be "p" = in progress
        crit.add(Restrictions.eq("sendStatus", "p"));
        return DbUtils.toList(crit, EmailItem.class);
    }
    private List<EmailSendAttempt> emailSendAttempts;
    private long id;
    private BaseEmailJob job; // optional, might be linked to a job
    private EmailTrigger emailTrigger;
    private Profile sender; // optional, the user account which sent the email if originated internally
    private BaseEntity recipient; // reference to user, possibly null
    private String recipientAddress; // actual email address being sent to
    private String fromAddress; // the stated from address
    private String replyToAddress; // reply-to field    
    private String subject; // subject line
    private String html; // body html, fully rendered. ie after applying template
    private String text; // body as plain text
    private Date createdDate;
    private String sendStatus; // status of the send job. c=completed, p=pending, r=retrying, f=failed
    private Date sendStatusDate;
    private Integer numAttempts;
    private Date nextAttempt;
    private boolean readStatus; // status of reading the email, if delivered to an internal user. false=not yet read
    private int messageSize;
    private String disposition;
    private String encoding;
    private String contentLanguage;
    private String toList;
    private String ccList;
    private String bccList;

    public EmailItem() {
    }
    
    
    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne
    public BaseEmailJob getJob() {
        return job;
    }

    public void setJob(BaseEmailJob job) {
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
    public BaseEntity getRecipient() {
        return recipient;
    }

    public void setRecipient(BaseEntity recipient) {
        this.recipient = recipient;
    }

    @Column(nullable = false)
    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    @Column(nullable = false)
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Column(nullable = false)
    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    @Column(nullable = false)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column(nullable = true, length = 20000)
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Column(nullable = true, length = 20000)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * The send status. Initially null, then set to: - f = failed - c =
     * completed - p = pending/in progress - r = retry
     *
     * @return
     */
    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    /**
     * Required field
     *
     * @return
     */
    @Column(nullable = false)
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

    public void setMessageSize(int size) {
        this.messageSize = size;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getDisposition() {
        return disposition;
    }

    public String getBccList() {
        return bccList;
    }

    public void setBccList(String bccList) {
        this.bccList = bccList;
    }

    public String getCcList() {
        return ccList;
    }

    public void setCcList(String ccList) {
        this.ccList = ccList;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getToList() {
        return toList;
    }

    public void setToList(String toList) {
        this.toList = toList;
    }

    /**
     * True if it was completed successfully. See getSendStatus for other states
     *
     * @return
     */
    public boolean complete() {
        return "c".equals(sendStatus);
    }

    public boolean failed() {
        return "f".equals(sendStatus);
    }

    @Column(nullable = true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getNextAttempt() {
        return nextAttempt;
    }

    public void setNextAttempt(Date nextAttempt) {
        this.nextAttempt = nextAttempt;
    }

    /**
     * Number of attempts at SMTP delivery
     *
     * @return
     */
    @Column
    public Integer getNumAttempts() {
        return numAttempts;
    }

    public void setNumAttempts(Integer numRetries) {
        this.numAttempts = numRetries;
    }

    @ManyToOne
    public EmailTrigger getEmailTrigger() {
        return emailTrigger;
    }

    public void setEmailTrigger(EmailTrigger emailTrigger) {
        this.emailTrigger = emailTrigger;
    }
}
