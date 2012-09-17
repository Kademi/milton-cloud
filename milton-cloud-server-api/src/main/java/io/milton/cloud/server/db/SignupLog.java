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

import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
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
 * Records a user signing up to a group
 *
 * @author brad
 */
@javax.persistence.Entity
public class SignupLog {

    public static void logSignup(Website website, Profile p, Organisation withinOrg, Group group, Session session) {
        logSignup(website, website.getOrganisation(), p, withinOrg, group, session);
    }
    public static void logSignup(Website website, Organisation org, Profile p, Organisation withinOrg, Group group, Session session) {
        SignupLog l = new SignupLog();
        l.setWebsite(website);
        l.setGroupEntity(group);
        l.setMembershipOrg(withinOrg);
        l.setOrganisation(org);
        l.setProfile(p);
        Date dt = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        l.setReqDate(new java.sql.Date(dt.getTime()));
        l.setReqYear(cal.get(Calendar.YEAR));
        l.setReqMonth(cal.get(Calendar.MONTH));
        l.setReqDay(cal.get(Calendar.DAY_OF_MONTH));
        l.setReqHour(cal.get(Calendar.HOUR_OF_DAY));
        session.save(l);
    }
    
    private long id;
    /**
     * The org that the user belongs to
     *
     */
    private Organisation organisation;
    /**
     * The organisation the group membership is associated with
     */
    private Organisation membershipOrg;
    /**
     * The website which recorded the signup, if any
     */
    private Website website;
    private Profile profile;
    private Group group;
    private Date reqDate;
    private int reqYear;
    private int reqMonth;
    private int reqDay;
    private int reqHour;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @ManyToOne
    public Group getGroupEntity() {
        return group;
    }

    public void setGroupEntity(Group group) {
        this.group = group;
    }

    @ManyToOne
    public Organisation getMembershipOrg() {
        return membershipOrg;
    }

    public void setMembershipOrg(Organisation membershipOrg) {
        this.membershipOrg = membershipOrg;
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
}
