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
package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a set of NvPair (ie Name/Value Pairs) which together form a set of
 * information about some item.
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NvSet implements Serializable{
    private Long id;
    private Long previousSetId;
    private Date createdDate;
    private Set<NvPair> nvPairs;
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column
    public Long getPreviousSetId() {
        return previousSetId;
    }

    public void setPreviousSetId(Long previousSetId) {
        this.previousSetId = previousSetId;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    
    
    @OneToMany
    public Set<NvPair> getNvPairs() {
        return nvPairs;
    }

    public void setNvPairs(Set<NvPair> nvPairs) {
        this.nvPairs = nvPairs;
    }
       
    public void delete(Session session) {
        if( getNvPairs() != null ) {
            for( NvPair nvp : getNvPairs()) {
                nvp.delete(session);
            }
            setNvPairs(new HashSet<NvPair>());
        }
                
        session.delete(this);
    }
    
    public NvSet previous(Session session) {
        if( previousSetId != null ) {
            return (NvSet) session.get(NvSet.class, previousSetId);
        }
        return null;
    }

    public NvSet duplicate(Date now, Session session) {
        NvSet nvSet = new NvSet();
        nvSet.setCreatedDate(now);
        nvSet.setNvPairs(new HashSet<NvPair>());
        nvSet.setPreviousSetId(this.getId());
        session.save(nvSet);
        if( getNvPairs() != null ) {
            for( NvPair nvp : getNvPairs()) {
                NvPair newNvp = nvSet.addPair(nvp.getName(), nvp.getPropValue());
                session.save(newNvp);
            }
        }
        return nvSet;
    }

    public NvPair addPair(String name, String propValue) {
        if( getNvPairs() == null ) {
            setNvPairs(new HashSet<NvPair>());
        }
        NvPair nvp = new NvPair();
        nvp.setName(name);
        nvp.setNvSet(this);
        nvp.setPropValue(propValue);
        getNvPairs().add(nvp);
        return nvp;
    }
    
    @Transient
    public boolean isEmpty() {
        return getNvPairs() == null || getNvPairs().isEmpty();
    }

    public void removePair(String name) {
        if( getNvPairs() == null ) {
            return ;
        }
        Iterator<NvPair> it = getNvPairs().iterator();
        while( it.hasNext()) {
            NvPair nvp = it.next();
            if( nvp.getName().equals(name)) {
                it.remove();
            }
        }
    }
}
