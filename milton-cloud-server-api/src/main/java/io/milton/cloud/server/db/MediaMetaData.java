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
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

/**
 * Given the hash for a primary media file (ie not for alternative formats) this
 * holds metadata such as duration, height and width and recorded date (not available
 * for all formats)
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sourceHash"})}
)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MediaMetaData implements Serializable {
    
    public static MediaMetaData find(String sourceHash, Session session) {
        Criteria crit = session.createCriteria(MediaMetaData.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("sourceHash", sourceHash));        
        return DbUtils.unique(crit);
    }    
    
    /**
     * Finds the record if it exists, otherwise creates it and sets the primary hash
     * 
     * Does not save if newly created
     * 
     * @param sourceHash
     * @param session
     * @return 
     */
    public static MediaMetaData getOrCreate( String sourceHash, Session session) {
        MediaMetaData f = find(sourceHash, session);
        if( f != null ) {
            return f;
        } else {
            f = new MediaMetaData();
        }        
        f.setSourceHash(sourceHash);
        return f;
    }    

    
    private long id;
    private String sourceHash; // the hash of the file from which the information was derived
    private Double durationSecs;
    private Date recordedDate;
    private Integer width;
    private Integer height;
    
    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }

    @Column
    public Double getDurationSecs() {
        return durationSecs;
    }

    public void setDurationSecs(Double durationSecs) {
        this.durationSecs = durationSecs;
    }

    @Column(nullable=true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)     
    public Date getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(Date recordedDate) {
        this.recordedDate = recordedDate;
    }

    @Column
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }

    @Column
    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    
    
    
}
