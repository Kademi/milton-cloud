package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;

/**
 * Sends a reminder to attendees of an invite at a certain amount of time
 * before (or after) an event
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Reminder {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Reminder.class);
    
    /**
     * Creates, but does not save, an instance
     * 
     * @param event
     * @param now
     * @return 
     */
    public static Reminder create(CalEvent event, Date now) {
        Reminder r = new Reminder();
        r.setCreatedDate(now);
        r.setEvent(event);
        return r;
    }
    
    public static List<Reminder> findDue(Date now, Session session) {
        // Just get everything that is enabled and not-yet run, and iterate over
        // them. Not very efficient, but should be low volume
        Criteria crit = session.createCriteria(Reminder.class);
        crit.add(Restrictions.eq("enabled", true));
        crit.add(Restrictions.isNull("processedDate"));        
        List<Reminder> list = DbUtils.toList(crit, Reminder.class);        
        List<Reminder> result = new ArrayList<>();
        for( Reminder r : list ) {
            if( r.due(now) ) {
                result.add(r);
            }
        }
        return result;
    }

    public enum TimeUnit {
        HOURS,
        DAYS,
        WEEKS,
        MONTHS,
        ANNUAL
    }    
    
    private long id;
    private CalEvent event; // the event this is a reminder for
    private Website themeSite; // if present, the email will use the email template from the website's live theme
    private TimeUnit timerUnit;
    private Integer timerMultiple; // eg run every 3 days
    private String subject;
    private String html;
    private boolean enabled;
    private Date processedDate; // not null means has been processed
    private Date createdDate;

    public Reminder() {
    }
    
    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public CalEvent getEvent() {
        return event;
    }

    public void setEvent(CalEvent event) {
        this.event = event;
    }
    
    @ManyToOne
    public Website getThemeSite() {
        return themeSite;
    }

    public void setThemeSite(Website themeSite) {
        this.themeSite = themeSite;
    }
    
    public TimeUnit getTimerUnit() {
        return timerUnit;
    }

    public void setTimerUnit(TimeUnit timerUnit) {
        this.timerUnit = timerUnit;
    }

    public Integer getTimerMultiple() {
        return timerMultiple;
    }

    public void setTimerMultiple(Integer timerMultiple) {
        this.timerMultiple = timerMultiple;
    }
    
    @Column
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column(length = 2048)
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }    
    
    public boolean getCreateTimerEnabled() {
        return enabled;
    }

    public void setCreateTimerEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }    

    /**
     * Null if this has not been processed yet, otherwise the date it was processed
     * 
     * @return 
     */
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(nullable = true)    
    public Date getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(Date processedDate) {
        this.processedDate = processedDate;
    }
    

    /**
     * Returns true if this reminder should be executed now
     * 
     * @param now
     * @return 
     */
    public boolean due(Date now) {
        // calculate the due date
        Date eventDate = getEvent().getStartDate();
        if( eventDate == null ) {
            return false;
        }
        Date dueDate = dueDate(eventDate);
        boolean b = dueDate.after(now);
        log.info("due? eventDate: {} dueDate: {} result=", eventDate, dueDate, b);
        return b;
    }
    
    public Date dueDate(Date eventDate) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(eventDate);
        switch (getTimerUnit() ) {
            case HOURS:
                cal.add(java.util.Calendar.HOUR, getTimerMultiple());
                break;
            
            case DAYS:                
                cal.add(java.util.Calendar.DATE, getTimerMultiple());
                break;
            case WEEKS:
                cal.add(java.util.Calendar.WEEK_OF_YEAR, getTimerMultiple());
                break;
            case MONTHS:                
                cal.add(java.util.Calendar.MONTH, getTimerMultiple());
                break;
            case ANNUAL:
                cal.add(java.util.Calendar.YEAR, getTimerMultiple());
                break;
            default:
                return null;
        }        
        return cal.getTime();
    }    
        
}
