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
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This table defines schedule jobs normally used to send emails at certain
 * times
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("S")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ScheduledEmail extends BaseEmailJob implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEmail.class);

    public static List<ScheduledEmail> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(ScheduledEmail.class);
        crit.add(Restrictions.eq("organisation", org));
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        crit.add(notDeleted);
        
        return DbUtils.toList(crit, ScheduledEmail.class);
    }

    public static List<ScheduledEmail> findPossiblyDue(Date now, Session session) {
        Criteria crit = session.createCriteria(ScheduledEmail.class);
        crit.add(Restrictions.eq("enabled", true));
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        crit.add(notDeleted);
        
        crit.add(Restrictions.lt("startDate", now));
        crit.add(
                Restrictions.or(Restrictions.gt("endDate", now), Restrictions.isNull("endDate")));
        return DbUtils.toList(crit, ScheduledEmail.class);
    }

    /**
     * Find a job which is due for execution and mark it as being in progress.
     *
     * @param org
     * @param now
     * @param session
     * @return
     */
    public static TakeResult takeDue(Date now, Session session) {
        for (ScheduledEmail e : findPossiblyDue(now, session)) {
            TakeResult takeResult = e.take(now, session);
            if (takeResult != null) {
                log.info("takeDue: Found scheduled email, job ID=" + takeResult.scheduledEmail.getId());
                return takeResult;
            }
        }
        return null;
    }

    public enum Frequency {
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY,
        ANNUAL
    }
    private List<ScheduledEmailResult> scheduledEmailResults;
    private Frequency frequency;
    private Date startDate;
    private Date endDate;
    private Profile runas;
    private boolean enabled;
    private int periodMultiples; // eg run every 3 days
    private int runHour; // eg run at 3am
    private String hrefTemplate; // a path or url to a web resource to attach or include, may include template vars
    private boolean attachHref; // if true the web resource is attached to the email, otherwise a link is inserted

    public ScheduledEmail() {
    }

    public String getHrefTemplate() {
        return hrefTemplate;
    }

    public void setHrefTemplate(String hrefTemplate) {
        this.hrefTemplate = hrefTemplate;
    }

    public boolean isAttachHref() {
        return attachHref;
    }

    public void setAttachHref(boolean attachHref) {
        this.attachHref = attachHref;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(nullable = false)
    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column()
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Column(nullable = false)
    public int getPeriodMultiples() {
        return periodMultiples;
    }

    public void setPeriodMultiples(int periodMultiples) {
        this.periodMultiples = periodMultiples;
    }

    @Column(nullable = false)
    public int getRunHour() {
        return runHour;
    }

    public void setRunHour(int runHour) {
        this.runHour = runHour;
    }

    @ManyToOne
    public Profile getRunas() {
        return runas;
    }

    public void setRunas(Profile runas) {
        this.runas = runas;
    }

    @Override
    public void accept(EmailJobVisitor visitor) {
        visitor.visit(this);
    }

    public List<EmailItem> history(Date from, Date to, boolean reverseOrder, Session session) {
        return EmailItem.findByJobAndDate(this, from, to, reverseOrder, session);
    }

    @Transient
    public List<EmailItem> getHistory() {
        Session session = SessionManager.session();
        return history(null, null, true, session);
    }

    @OneToMany(mappedBy = "scheduledEmail")
    public List<ScheduledEmailResult> getScheduledEmailResults() {
        return scheduledEmailResults;
    }

    public void setScheduledEmailResults(List<ScheduledEmailResult> scheduledEmailResults) {
        this.scheduledEmailResults = scheduledEmailResults;
    }

    public ScheduledEmailResult getLastResult(Session session) {
        return ScheduledEmailResult.findLatest(this, session);
    }

    public Date lastRun(Date now, Session session) {
        Date lastRun = null;
        ScheduledEmailResult result = getLastResult(session);
        while (result != null && lastRun == null) {
            if (result.getCompletedDate() == null) {
                if (result.isExpired(now)) {
                    result = getLastResult(session);
                } else {
                    lastRun = result.getStartDate();
                    break;
                }
            } else {
                lastRun = result.getStartDate();
                break;
            }
        }
        if (lastRun == null) {
            lastRun = getStartDate();
        }
        return lastRun;
    }    
    
    public Date nextDue(Date now, Session session) {
        Date lastRun = lastRun(now, session);
        Date nextRun = nextRun(lastRun);        
        return nextRun;
    }
    
    public TakeResult take(Date now, Session session) {
        ScheduledEmailResult result = getLastResult(session);
        Date lastRun = null;
        while (result != null && lastRun == null) {
            if (result.getCompletedDate() == null) {
                if (result.isExpired(now)) {
                    log.warn("Found an expired result: " + result.getId());
                    session.delete(result);
                    result = getLastResult(session);
                } else {
                    lastRun = result.getStartDate();
                    log.info("found last run start date: " + lastRun);
                    break;
                }
            } else {
                log.info("last run is completed");
                lastRun = result.getStartDate();
                break;
            }
        }
        if (lastRun == null) {
            lastRun = getStartDate();
            log.info("No last run date from previous run, use task start date=" + lastRun);
        } else {
            log.info("Use started date from last result as last run date=" + lastRun);
        }

        // next due run
        Date nextRun = nextRun(lastRun);
        if (nextRun == null) {
            log.warn("Could not calculate a next run date for id: " + getId());
            return null;
        } else {
            log.info("Calculated nextRun due at: " + nextRun);
            if (now.after(nextRun)) {
                ScheduledEmailResult thisResult = new ScheduledEmailResult();
                thisResult.setStartDate(now);
                thisResult.setScheduledEmail(this);
                if (getScheduledEmailResults() == null) {
                    setScheduledEmailResults(new ArrayList<ScheduledEmailResult>());
                }
                getScheduledEmailResults().add(thisResult);
                session.save(thisResult);

                TakeResult takeResult = new TakeResult();
                takeResult.setPreviousResult(result);
                takeResult.setScheduledEmail(this);
                takeResult.setThisResult(thisResult);                
                
                return takeResult;
            } else {
                return null;
            }
        }
    }

    public Date nextRun(Date lastRun) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastRun);
        switch (getFrequency()) {
            case HOURLY:
                cal.add(Calendar.HOUR, getPeriodMultiples());
                break;
            
            case DAILY:                
                cal.add(Calendar.DATE, getPeriodMultiples());
                cal.set(Calendar.HOUR_OF_DAY, getRunHour());
                break;
            case WEEKLY:                
                cal.add(Calendar.WEEK_OF_YEAR, getPeriodMultiples());
                cal.set(Calendar.HOUR_OF_DAY, getRunHour());
                break;
            case MONTHLY:                
                cal.add(Calendar.MONTH, getPeriodMultiples());
                cal.set(Calendar.HOUR_OF_DAY, getRunHour());
                break;
            case ANNUAL:
                cal.add(Calendar.YEAR, getPeriodMultiples());
                cal.set(Calendar.HOUR_OF_DAY, getRunHour());
                break;
            default:
                return null;
        }        
        return cal.getTime();
    }

    @Transient
    @Override
    public boolean isActive() {
        return enabled && !deleted();
    }
    
    

    public static class TakeResult {

        ScheduledEmailResult previousResult;
        ScheduledEmailResult thisResult;
        ScheduledEmail scheduledEmail;

        public ScheduledEmailResult getPreviousResult() {
            return previousResult;
        }

        public ScheduledEmail getScheduledEmail() {
            return scheduledEmail;
        }

        public ScheduledEmailResult getThisResult() {
            return thisResult;
        }

        public void setPreviousResult(ScheduledEmailResult previousResult) {
            this.previousResult = previousResult;
        }

        public void setScheduledEmail(ScheduledEmail scheduledEmail) {
            this.scheduledEmail = scheduledEmail;
        }

        public void setThisResult(ScheduledEmailResult thisResult) {
            this.thisResult = thisResult;
        }
    }
}
