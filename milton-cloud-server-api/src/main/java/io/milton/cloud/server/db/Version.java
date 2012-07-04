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

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import javax.persistence.*;
import org.apache.commons.io.output.NullOutputStream;
import org.hibernate.Session;

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

    public static long calcModHash(long versionHash, long modDate, long modProfileId) {
        NullOutputStream out = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(out, new Adler32());
        String hashableText = versionHash + ":" + modDate + ":" + modProfileId;
        try {
            cout.write(hashableText.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        Checksum check = cout.getChecksum();
        return check.getValue();
    }

    public static void insert(long previousResourceHash, Date previousModDate, long previousProfileId, long newResourceHash, Date newModDate, long newProfileId, Session session) {
        long previousModDateLong = previousModDate == null ? 0 : previousModDate.getTime();
        long previousModHash = calcModHash(previousResourceHash, previousModDateLong, previousProfileId);
        long newModHash = calcModHash(newResourceHash, newModDate.getTime(), newProfileId);
        Version v = new Version();
        v.setModHash(newModHash);
        v.setPreviousModHash(previousModHash);
        v.setModDate(newModDate);
        v.setProfileId(newProfileId);
        v.setResourceHash(newResourceHash);
    }
    
    private long id;
    private long modHash;
    private long previousModHash;
    private long profileId;
    private Date modDate;
    private long resourceHash;

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
    public long getResourceHash() {
        return resourceHash;
    }

    public void setResourceHash(long versionHash) {
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
    public long getPreviousModHash() {
        return previousModHash;
    }

    public void setPreviousModHash(long previousHash) {
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
    public long getModHash() {
        return modHash;
    }

    public void setModHash(long modHash) {
        this.modHash = modHash;
    }

}
