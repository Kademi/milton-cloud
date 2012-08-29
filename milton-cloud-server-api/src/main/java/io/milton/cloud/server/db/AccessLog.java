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
import io.milton.vfs.db.Website;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import org.hibernate.Session;

/**
 * Records an HTTP access to the system
 *
 * @author brad
 */
@javax.persistence.Entity
public class AccessLog {

    public static void insert(Organisation org, Website website, String host, String url, String referrerUrl, int result, long duration, Long size, String method, String contentType, String fromAddress, String user, Session session) {
        AccessLog al = new AccessLog();
        al.setOrganisation(org);
        al.setWebsite(website);
        al.setReqHost(host);
        al.setUrl(url);
        al.setReferrer(referrerUrl);
        Date dt = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        al.setReqDate(new java.sql.Date(dt.getTime()));
        al.setReqYear(cal.get(Calendar.YEAR));
        al.setReqMonth(cal.get(Calendar.MONTH));
        al.setReqDay(cal.get(Calendar.DAY_OF_MONTH));
        al.setReqHour(cal.get(Calendar.HOUR_OF_DAY));
        al.setResultCode(result);
        al.setDurationMs(duration);
        al.setNumBytes(size);
        al.setReqMethod(method);
        al.setContentType(contentType);
        al.setReqFrom(fromAddress);
        al.setReqUser(user);
        session.save(al);

    }
    private long id;
    private Organisation organisation;
    private Website website;
    private String reqHost;
    private String url;
    private String referrer;
    private Date reqDate;
    private int reqYear;
    private int reqMonth;
    private int reqDay;
    private int reqHour;
    private int resultCode;
    private long durationMs;
    private Long numBytes;
    private String reqMethod;
    private String contentType;
    private String reqFrom;
    private String reqUser;

    public AccessLog() {
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
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @ManyToOne
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public String getReqHost() {
        return reqHost;
    }

    public void setReqHost(String reqHost) {
        this.reqHost = reqHost;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getReqDate() {
        return reqDate;
    }

    public void setReqDate(Date reqDate) {
        this.reqDate = reqDate;
    }

    public int getReqYear() {
        return reqYear;
    }

    public void setReqYear(int reqYear) {
        this.reqYear = reqYear;
    }

    public int getReqMonth() {
        return reqMonth;
    }

    public void setReqMonth(int reqMonth) {
        this.reqMonth = reqMonth;
    }

    public int getReqDay() {
        return reqDay;
    }

    public void setReqDay(int reqDay) {
        this.reqDay = reqDay;
    }

    public int getReqHour() {
        return reqHour;
    }

    public void setReqHour(int reqHour) {
        this.reqHour = reqHour;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * The duration of the processing time. Note that this is the time to
     * process the request, but does not include time to transmit to the client
     *
     * @return
     */
    @Column
    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * The size of the response entity, if known. Will often not be known for
     * generated data such as HTML pages, so will be null
     *
     * @return
     */
    @Column
    public Long getNumBytes() {
        return numBytes;
    }

    public void setNumBytes(Long numBytes) {
        this.numBytes = numBytes;
    }

    /**
     * The request method, eg GET, POST
     *
     * @return
     */
    @Column(nullable = false)
    public String getReqMethod() {
        return reqMethod;
    }

    public void setReqMethod(String reqMethod) {
        this.reqMethod = reqMethod;
    }

    /**
     * The content type of the response
     *
     * @return
     */
    @Column
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * The IP address the request is from
     *
     * @return
     */
    @Column
    public String getReqFrom() {
        return reqFrom;
    }

    public void setReqFrom(String reqFrom) {
        this.reqFrom = reqFrom;
    }

    @Column
    public String getReqUser() {
        return reqUser;
    }

    public void setReqUser(String reqUser) {
        this.reqUser = reqUser;
    }
}
