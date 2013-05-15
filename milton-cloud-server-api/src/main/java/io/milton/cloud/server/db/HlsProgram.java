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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.IndexColumn;

/**
 * Represents a particular format of a video in HLS form. Usually will be different
 * formats for different resolutions
 *
 * @author brad
 */
@javax.persistence.Entity
public class HlsProgram implements Serializable {

    public static HlsProgram get(long progId, Session session) {
        return (HlsProgram) session.get(HlsProgram.class, progId);
    }
        
    private long id;
    private HlsPrimary primaryPlaylist;
    private List<HlsSegment> segments;
    private Integer height;
    private Integer width;
    private Integer bandwidth;
    
    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    @IndexColumn(name = "hlsprg_pri")
    public HlsPrimary getPrimaryPlaylist() {
        return primaryPlaylist;
    }

    public void setPrimaryPlaylist(HlsPrimary primary) {
        this.primaryPlaylist = primary;
    }
      

    public Integer getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @OneToMany(mappedBy = "program")
    @OrderBy("sequenceNum ASC")
    public List<HlsSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<HlsSegment> segments) {
        this.segments = segments;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public HlsSegment addSegment(String segmentHash, int sequence) {
        if( getSegments() == null ) {
            setSegments(new ArrayList<HlsSegment>());
        }
        HlsSegment seg = new HlsSegment();
        seg.setProgram(this);
        seg.setSegmentHash(segmentHash);
        seg.setSequenceNum(sequence);
        getSegments().add(seg);
        return seg;
    }

    public void delete(Session session) {
        if( segments != null ) {
            Iterator<HlsSegment> it = segments.iterator();
            while( it.hasNext() ) {
                it.next().delete(session);
                it.remove();
            }
        }
        session.delete(this);
    }

    
    
}
