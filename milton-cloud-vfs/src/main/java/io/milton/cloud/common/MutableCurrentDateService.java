package io.milton.cloud.common;

import java.util.Date;

/**
 * Allows modification of the "current" date for testing purposes
 *
 * @author brad
 */
public class MutableCurrentDateService extends DefaultCurrentDateService implements CurrentDateService {

    private Date artificalDate = null;
    private Long dateOffset = null;

    @Override
    public Date getNow() {
        if (artificalDate != null) {
            return artificalDate;
        } else if (dateOffset != null) {
            long tm = System.currentTimeMillis() + dateOffset;
            return new Date(tm);
        } else {
            return new Date();
        }

    }

    public Date getArtificalDate() {
        return artificalDate;
    }

    public void setArtificalDate(Date artificalDate) {
        this.artificalDate = artificalDate;
    }

    /**
     * Number of milliseconds to offset the actual datetime by
     *
     * @return
     */
    public Long getDateOffset() {
        return dateOffset;
    }

    public void setDateOffset(Long dateOffset) {
        this.dateOffset = dateOffset;
    }

    public void addHours(long d) {
        setDateOffset(1000 * 60 * 60 * d);
    }

    /**
     * Set the current date offset to 1 day
     */
    public void addDay() {
        setDateOffset(1000 * 60 * 60 * 24l);
    }

    public void addDays(long d) {
        setDateOffset(1000 * 60 * 60 * 24 * d);
    }

    /**
     * Set the current date offset to 1 week
     */
    public void addWeek() {
        setDateOffset(1000 * 60 * 60 * 24 * 7l);
    }

    public void addWeeks(long i) {
        setDateOffset(1000 * 60 * 60 * 24 * 7 * i);
    }
}
