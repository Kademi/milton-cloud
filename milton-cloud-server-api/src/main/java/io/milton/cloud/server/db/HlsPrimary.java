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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.criterion.Restrictions;


/**
 * Represents a HLS format video which will have 1 or more formats
 * 
 * It may, optionally, be associated with a hash. If not associated with a hash
 * then it must be referenced from elsewhere to be useful. Eg if it represents
 * a live stream then there will be a live stream record pointing to this
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "sourceHash")})
public class HlsPrimary implements Serializable {

    public static HlsPrimary get(long hlsPrimaryId, Session session) {
        return (HlsPrimary) session.get(HlsPrimary.class, hlsPrimaryId);
    }

    public static HlsPrimary findByHash(String sourceHash, Session session) {
        Criteria crit = session.createCriteria(HlsPrimary.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("sourceHash", sourceHash));        
        return DbUtils.unique(crit);
    }
        
    private long id;
    private List<HlsProgram> programs;
    private String sourceHash;
    private boolean complete;
    private int targetDuration;    
    
    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column
    @IndexColumn(name = "hlspri_src")    
    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }
    
    public int getTargetDuration() {
        return targetDuration;
    }

    public void setTargetDuration(int targetDuration) {
        this.targetDuration = targetDuration;
    }      

    @OneToMany(mappedBy = "primaryPlaylist")
    public List<HlsProgram> getPrograms() {
        return programs;
    }

    public void setPrograms(List<HlsProgram> programs) {
        this.programs = programs;
    }               

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
    
    

    public HlsProgram addProgram(int width, int height, Integer likelyBandwidth) {
        if( programs == null ) {
            programs = new ArrayList<>();
        }
        HlsProgram p = new HlsProgram();
        programs.add(p);
        p.setPrimaryPlaylist(this);
        p.setBandwidth(likelyBandwidth);
        p.setHeight(height);
        p.setWidth(width);
        return p;
    }

    public void delete(Session session) {
        if( programs != null ) {
            Iterator<HlsProgram> it = programs.iterator();
            while( it.hasNext()) {
                it.next().delete(session);
                it.remove();
            }
        }
        session.delete(this);
    }
}
