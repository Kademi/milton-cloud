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
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A NvPair is to allow data capture for entities such as users, groups and organisations
 * 
 * Every NvPair is associated with a NvSet. The owning entity should have a reference
 * to the set. NvPairs and sets should be considered immutable, so instead of updating
 * the value, create a new set with a new NvPair for each item, and then link
 * the owning entity to the set. Remember to set the previousSetId on the new
 * set to the previous set to allow navigating old versions
 * 
 * The definition of what needs to be captured is held elsewhere.
 * 
 * Values are stored in a string representation. Knowledge of the type of the data
 * in the data capture definition is required to parse the value
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NvPair implements Serializable{
    private Long id;
    private NvSet nvSet;
    private String name;
    private String propValue;
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }    
    
    @Column(nullable=false)
    public String getPropValue() {
        return propValue;
    }

    public void setPropValue(String propValue) {
        this.propValue = propValue;
    }

    @ManyToOne(optional = false)
    public NvSet getNvSet() {
        return nvSet;
    }

    public void setNvSet(NvSet nvSet) {
        this.nvSet = nvSet;
    }

    

    
    
    public void delete(Session session) {
        session.delete(this);
    }
    
    
}
