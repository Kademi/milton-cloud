/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.UniqueConstraint;
import org.hibernate.Session;

/**
 * Created when a user requests a password reset
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"token"})}
)
public class PasswordReset implements Serializable {
    
    public static PasswordReset create(Profile p, Date now, String returnUrl, Website web, Session session) {
        PasswordReset r = new PasswordReset();
        r.setCreatedDate(now);
        r.setProfile(p);
        r.setReturnUrl(returnUrl);
        r.setWebsite(web);
        r.setToken(UUID.randomUUID().toString());
        session.save(r);
        return r;
    }
    
    private long id;
    private Website website; // the website on which the request was received, null if via admin console
    private String returnUrl; // the url the user should be directed to for resetting their password
    private Profile profile; // the user account to reset
    private Date createdDate;
    private String token; // the unique token representing this request

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)        
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable=false)
    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    @ManyToOne(optional=false)
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    
}
