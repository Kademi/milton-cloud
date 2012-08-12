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

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

/**
 * Holds information about a version of a resource.
 *
 * Also holds a reference to the previous Version record
 * 
 * Version records are identified by the "modHash", which is a hash of the identifiers
 * which identify the version:
 *  - the hash of the resource
 *  - the modification date
 *  - the user (profile) id of the user that made the change
 * 
 * Note that the term "modHash" is not the same as the hash of a resource
 *
 * @author brad
 */
@Entity
public class Version implements Serializable {

    public static String calcModHash(String versionHash, long modDate, long modProfileId) {
        String hashableText = versionHash + ":" + modDate + ":" + modProfileId;
        return hashableText;
    }

    public static void insert(String previousResourceHash, Date previousModDate, long previousProfileId, String newResourceHash, Date newModDate, long newProfileId, Session session) {
        if( newResourceHash == null ) {
            //throw new RuntimeException("newResourceHash cannot be null");
            return ;
        }
        long previousModDateLong = previousModDate == null ? 0 : previousModDate.getTime();
        String previousModHash = calcModHash(previousResourceHash, previousModDateLong, previousProfileId);
        String newModHash = calcModHash(newResourceHash, newModDate.getTime(), newProfileId);
        Version v = new Version();
        v.setModHash(newModHash);
        v.setPreviousModHash(previousModHash);
        v.setModDate(newModDate);
        v.setProfileId(newProfileId);
        v.setResourceHash(newResourceHash);
        session.save(v);
    }
    
    public static Version find(String currentHash, Date currentModDate, long modUserId, Session session) {
        String modHash = calcModHash(currentHash, currentModDate.getTime(), modUserId);
        Criteria crit = session.createCriteria(Version.class);
        crit.add(Restrictions.eq("modHash", modHash));
        return DbUtils.unique(crit);
    }
    
    public static Version find(String modHash, Session session) {
        Criteria crit = session.createCriteria(Version.class);
        crit.add(Restrictions.eq("modHash", modHash));
        return DbUtils.unique(crit);
    }    
    
    private long id;
    private String modHash;
    private String previousModHash;
    private long profileId;
    private Date modDate;
    private String resourceHash;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * The hash of the version that this record represents
     * 
     * @return 
     */
    @Column(nullable = false)
    public String getResourceHash() {
        return resourceHash;
    }

    public void setResourceHash(String versionHash) {
        this.resourceHash = versionHash;
    }

    /**
     * The previousModHash is the modHash of the previous version of this resource.
     * It is NOT the hash of the previous resource, but the hash of the version
     * identifiers
     * 
     * @return 
     */
    @Column(nullable = false)
    public String getPreviousModHash() {
        return previousModHash;
    }

    public void setPreviousModHash(String previousHash) {
        this.previousModHash = previousHash;
    }
    
    

    @Column(nullable = false)
    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    
    

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    @Column(nullable = false)    
    @Index(name="idx_version_modhash")
    public String getModHash() {
        return modHash;
    }

    public void setModHash(String modHash) {
        this.modHash = modHash;
    }

    public Version previousVersion(Session session) {
        Version prev = find(getPreviousModHash(), session);
        return prev;
    }

}
