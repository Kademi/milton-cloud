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
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.IndexColumn;

/**
 * Represents a segment within a HLS program. This is a little piece of
 * video/audio at the resolution and approx bitrate given by the program
 *
 * @author brad
 */
@javax.persistence.Entity
public class HlsSegment implements Serializable {
        
    private long id;
    private HlsProgram program;
    private String segmentHash;
    private Double durationSecs;
    private int sequenceNum;
    
    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    @IndexColumn(name = "hlsseg_prg")
    public HlsProgram getProgram() {
        return program;
    }

    public void setProgram(HlsProgram program) {
        this.program = program;
    }

    
    
    @Column
    public Double getDurationSecs() {
        return durationSecs;
    }

    public void setDurationSecs(Double durationSecs) {
        this.durationSecs = durationSecs;
    }

    @Column(nullable = false)
    public String getSegmentHash() {
        return segmentHash;
    }

    public void setSegmentHash(String segmentHash) {
        this.segmentHash = segmentHash;
    }

    @Column
    public int getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(int sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public void delete(Session session) {
        session.delete(this);
    }

    
    
    
    
}
