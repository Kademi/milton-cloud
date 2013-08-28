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
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.LogicalExpression;
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
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EmailItem implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(EmailItem.class);
    public static final String STATUS_RETRY = "r";
    public static final String STATUS_PENDING = "p";
    public static final String STATUS_COMPLETE = "c";
    public static final String STATUS_FAILED = "f";

    /**
     *
     * @param session
     * @return
     */
    public static List<EmailItem> findToSend(Date now, Session session) {
        int MAX = 10;
        List<EmailItem> results = new ArrayList<>();
        results.addAll(findToSendWithNoJob(MAX, session));
        if (!results.isEmpty()) {
            log.info("findToSend: found non-job items: " + results.size());
        }
        if (results.size() >= MAX) {
            return results;
        }
        // Find jobs with email items to send
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.isNotNull("job"));
        crit.add(Restrictions.or(
                Restrictions.isNull("sendStatus"),
                Restrictions.eq("sendStatus", STATUS_RETRY)));
        crit.setProjection(Projections.groupProperty("job"));
        List list = crit.list();
        if (list != null && !list.isEmpty()) {
            int jobMax = MAX / list.size(); // eg, if limit is 10, and there are 5 jobs, load 2 from each job
            if (jobMax < 1) {
                jobMax = 1;
            }
            log.info("Found " + list.size() + " jobs in progress. Use job size: " + jobMax);
            for (Object oJob : list) {
                BaseEmailJob job = (BaseEmailJob) oJob;
                List<EmailItem> jobItems = findToSendWithJob(job, jobMax, session);
                results.addAll(jobItems);
            }
        } else {
            log.info("Found no in progress jobs");
        }
        return results;
    }

    /**
     * Finds email items ready to send which are not related to a Job, which are
     * going to be created individually. This is to allow the queue processing
     * logic to prioritise these ahead of batch jobs
     *
     * @param now
     * @param session
     * @return
     */
    public static List<EmailItem> findToSendWithNoJob(Integer maxResults, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.isNull("job"));
        crit.add(Restrictions.or(
                Restrictions.isNull("sendStatus"),
                Restrictions.eq("sendStatus", STATUS_RETRY)));
        crit.addOrder(Order.asc("createdDate"));
        if (maxResults != null) {
            crit.setMaxResults(maxResults);
        }
        return DbUtils.toList(crit, EmailItem.class);
    }

    /**
     * Find EmailItems which are ready to send and linked to the given job
     *
     * @param job
     * @param maxResults
     * @param session
     * @return
     */
    public static List<EmailItem> findToSendWithJob(BaseEmailJob job, Integer maxResults, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.eq("job", job));
        crit.add(Restrictions.or(
                Restrictions.isNull("sendStatus"),
                Restrictions.eq("sendStatus", STATUS_RETRY)));
        crit.addOrder(Order.asc("createdDate"));
        if (maxResults != null) {
            crit.setMaxResults(maxResults);
        }
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static List<EmailItem> findByRecipient(BaseEntity p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.eq("recipient", p));
        crit.addOrder(Order.desc("sendStatusDate"));
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static List<EmailItem> findByRecipientAndOrg(Organisation org, BaseEntity p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        Criteria critJob = crit.createCriteria("job", "j", Criteria.LEFT_JOIN);
        Criteria critTrigger = crit.createCriteria("emailTrigger", "t", Criteria.LEFT_JOIN);

        crit.add(Restrictions.eq("recipient", p));
        LogicalExpression orgRestrictions = Restrictions.or(Restrictions.eq("j.organisation", org), Restrictions.eq("t.organisation", org));
        crit.add(orgRestrictions);
        crit.addOrder(Order.desc("sendStatusDate"));
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static long findByNumUnreadByRecipient(Profile p, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        crit.add(Restrictions.eq("recipient", p));
        crit.add(Restrictions.eq("readStatus", false));
        crit.add(
                Restrictions.or(Restrictions.eq("hidden", false), Restrictions.isNull("hidden")));
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
        crit.add(Restrictions.eq("sendStatus", STATUS_PENDING));
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static List<EmailItem> findByJobAndDate(BaseEmailJob job, Date from, Date to, boolean orderReverseDate, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        // sendStatus must be "p" = in progress
        crit.add(Restrictions.eq("job", job));
        if (from != null) {
            crit.add(Restrictions.gt("createdDate", from));
        }
        if (to != null) {
            crit.add(Restrictions.le("createdDate", to));
        }
        if (orderReverseDate) {
            crit.addOrder(Order.desc("createdDate"));
        } else {
            crit.addOrder(Order.asc("createdDate"));
        }
        return DbUtils.toList(crit, EmailItem.class);
    }

    public static long findIncompleteByJob(BaseEmailJob job, Session session) {
        Criteria crit = session.createCriteria(EmailItem.class);
        // sendStatus must be "p" = in progress
        crit.add(Restrictions.eq("job", job));
        Disjunction inProg = Restrictions.disjunction();
        inProg.add(Restrictions.isNull("sendStatus"));
        inProg.add(Restrictions.eq("sendStatus", STATUS_RETRY));
        inProg.add(Restrictions.eq("sendStatus", STATUS_PENDING));
        crit.add(inProg);
        crit.setProjection(Projections.count("id"));
        Object result = crit.uniqueResult();
        if( result == null ) {
            return 0;
        } else if( result instanceof Integer ) {
            Integer i = (Integer) result;
            return i.longValue();
        } else {
            return (long) result;
        }
    }
    private long id;
    private List<EmailSendAttempt> emailSendAttempts;
    private List<EmailAttachment> attachments;
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
    private Boolean hidden; // do not show on user's inbox page
    private Boolean forceSend; // if true send even if job is not active. Used for preview emails

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

    @OneToMany(mappedBy = "emailItem")
    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<EmailAttachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Optional reference to the job which caused this to be sent
     *
     * @return
     */
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
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

    @Column
    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public void setForceSend(Boolean forceSend) {
        this.forceSend = forceSend;
    }

    /**
     * If true will be sent even if job is not active. If there is no job has no
     * effect
     *
     * @return
     */
    public Boolean getForceSend() {
        return forceSend;
    }

    /**
     * Null safe accessor for getForceSend
     *
     * @return
     */
    public boolean forceSend() {
        return forceSend != null && forceSend.booleanValue();
    }

    public boolean hidden() {
        if (getHidden() == null) {
            return false;
        }
        return getHidden().booleanValue();
    }

    public void addAttachment(String fileName, String hash, String contentType, String disposition, Session session) {
        EmailAttachment att = new EmailAttachment();
        att.setEmailItem(this);
        if (getAttachments() == null) {
            setAttachments(new ArrayList<EmailAttachment>());
        }
        getAttachments().add(att);
        att.setContentType(contentType);
        att.setDisposition(disposition);
        att.setFileHash(hash);
        att.setFileName(fileName);
        session.save(att);
    }

    /**
     * Is the sendStatus null or STATUS_READY
     *
     * @return
     */
    @Transient
    public boolean isReadyToSend() {
        return getSendStatus() == null || getSendStatus().equals(STATUS_RETRY);
    }

    public void delete(Session session) {
        if (this.attachments != null) {
            for (EmailAttachment a : attachments) {
                a.delete(session);
            }
        }
        if (this.emailSendAttempts != null) {
            for (EmailSendAttempt a : emailSendAttempts) {
                a.delete(session);
            }
        }
        session.delete(this);
    }

    @Transient
    public String getStatusText() {
        if (sendStatus == null) {
            return "queued";
        } else {
            switch (sendStatus) {
                case STATUS_COMPLETE:
                    return "complete";
                case STATUS_FAILED:
                    return "failed";
                case STATUS_PENDING:
                    return "pending";
                case STATUS_RETRY:
                    return "retrying";
            }
        }
        return sendStatus;
    }
}
