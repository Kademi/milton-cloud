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

import static io.milton.cloud.server.db.EmailTrigger.TimeUnit.ANNUAL;
import static io.milton.cloud.server.db.EmailTrigger.TimeUnit.DAYS;
import static io.milton.cloud.server.db.EmailTrigger.TimeUnit.HOURS;
import static io.milton.cloud.server.db.EmailTrigger.TimeUnit.MONTHS;
import static io.milton.cloud.server.db.EmailTrigger.TimeUnit.WEEKS;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires an event when a certain date/time is reached
 *
 * @author brad
 */
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TriggerTimer implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TriggerTimer.class);

    public static TriggerTimer create(EmailTrigger emailTrigger, Organisation org, Website website, Profile currentUser, Date now, String createdReason, Session session) {
        Date fireAt = timerFireAt(now, emailTrigger);
        TriggerTimer tt = new TriggerTimer();
        tt.setEmailTrigger(emailTrigger);
        tt.setOrganisation(org);
        tt.setFireForProfile(currentUser);
        tt.setCreatedAt(now);
        tt.setFireAt(fireAt);
        tt.setWebsite(website);
        tt.setNotes(createdReason);
        session.save(tt);
        return tt;
    }

    public static Date timerFireAt(Date now, EmailTrigger trigger) {
        if (trigger.getTimerUnit() == null) {
            log.warn("Null timer unit for trigger: " + trigger.getId() + " - " + trigger.getName());
            return now;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int units = trigger.getTimerMultiple();
        switch (trigger.getTimerUnit()) {
            case HOURS:
                cal.add(Calendar.HOUR, units);
                break;

            case DAYS:
                cal.add(Calendar.DATE, units);
                break;
            case WEEKS:
                cal.add(Calendar.WEEK_OF_YEAR, units);
                break;
            case MONTHS:
                cal.add(Calendar.MONTH, units);
                break;
            case ANNUAL:
                cal.add(Calendar.YEAR, units);
                break;
            default:
                return null;
        }
        return cal.getTime();
    }

    /**
     * Find up to maxResults of timers due to be processed and lock them, and
     * return their ID's. They should then be processed in seperate transactions
     *
     * @param now
     * @param timeoutMins
     * @param maxAttempts
     * @param session
     * @return
     */
    public static List<Long> takeDue(Date now, int maxResults, int timeoutMins, int maxAttempts, Session session) {
        List<Long> ids = new ArrayList<>();
        for (TriggerTimer e : findPossiblyDue(now, maxResults, timeoutMins, maxAttempts, session)) {
            e.take(now, session);
            ids.add(e.getId());
        }
        return ids;
    }

    public static List<TriggerTimer> findPossiblyDue(Date now, int maxResults, int timeoutMins, int maxAttempts, Session session) {
        Criteria crit = session.createCriteria(TriggerTimer.class);
        crit.setMaxResults(maxResults);
        crit.setLockMode(LockMode.PESSIMISTIC_WRITE);
        
        Criteria critEmailTrigger = crit.createCriteria("emailTrigger");
        critEmailTrigger.add(Restrictions.eq("enabled", true));
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        critEmailTrigger.add(notDeleted);

        Date timeoutTime = DateUtils.addMinutes(now, -1 * timeoutMins);

        crit.add(Restrictions.le("fireAt", now)); // is due        
        crit.add(Restrictions.isNull("completedProcessingAt")); // not completed
        crit.add(Restrictions.lt("numAttempts", maxAttempts)); // not already reached max attempts        
        Disjunction notTaken = Restrictions.disjunction(); // and not locked for processing
        notTaken.add(Restrictions.isNull("processingStartedAt"));
        notTaken.add(Restrictions.lt("processingStartedAt", timeoutTime));
        crit.add(notTaken);

        return DbUtils.toList(crit, TriggerTimer.class);
    }

    public static TriggerTimer get(Long id, Session session) {
        return (TriggerTimer) session.get(TriggerTimer.class, id);
    }

    public static List<TriggerTimer> search(Organisation organisation, Date from, Date to, Session session) {
        Criteria crit = session.createCriteria(TriggerTimer.class);
        crit.add(Restrictions.eq("organisation", organisation));
        if (from != null) {
            System.out.println("created at > " + from);
            crit.add(Restrictions.gt("createdAt", from));
        }
        if (to != null) {
            System.out.println("created at < " + to);
            crit.add(Restrictions.le("createdAt", to));
        }
        return DbUtils.toList(crit, TriggerTimer.class);
    }
    private long id;
    private EmailTrigger emailTrigger; // what caused this
    private Website website;
    private Organisation organisation;
    private Date createdAt;
    private Date fireAt;
    private Profile fireForProfile;
    private String notes;
    private Date processingStartedAt;
    private int numAttempts;
    private Date completedProcessingAt;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public EmailTrigger getEmailTrigger() {
        return emailTrigger;
    }

    public void setEmailTrigger(EmailTrigger emailTrigger) {
        this.emailTrigger = emailTrigger;
    }

    public int getNumAttempts() {
        return numAttempts;
    }

    public void setNumAttempts(int numAttempts) {
        this.numAttempts = numAttempts;
    }

    @ManyToOne(optional = false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * The date and time that this should fire at. In reality it wont fire at
     * exactly this time, but will be some time afterwards
     *
     * @return
     */
    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getFireAt() {
        return fireAt;
    }

    public void setFireAt(Date fireAt) {
        this.fireAt = fireAt;
    }

    /**
     * When a process begins processing it will immediately set and commit this
     * property, which should prevent any other processes from taking it for
     * some duration
     *
     * @return
     */
    @Column(nullable = true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(Date processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    /**
     * Set when the timer has been successfully processed
     *
     * @return
     */
    @Column(nullable = true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCompletedProcessingAt() {
        return completedProcessingAt;
    }

    public void setCompletedProcessingAt(Date completedProcessingAt) {
        this.completedProcessingAt = completedProcessingAt;
    }

    @ManyToOne(optional = false)
    public Profile getFireForProfile() {
        return fireForProfile;
    }

    public void setFireForProfile(Profile fireForProfile) {
        this.fireForProfile = fireForProfile;
    }

    @ManyToOne(optional = false)
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website w) {
        this.website = w;
    }

    @Column(length = 2048)
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void delete(Session session) {
        session.delete(this);
    }

    public void take(Date now, Session session) {
        this.setProcessingStartedAt(now);
        this.setNumAttempts(numAttempts++);
        session.save(this);
        session.flush();
    }
}
