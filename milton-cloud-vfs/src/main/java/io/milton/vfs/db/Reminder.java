package io.milton.vfs.db;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Sends a reminder to attendees of an invite at a certain amount of time
 * before (or after) an event
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Reminder {
    
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
    
}
