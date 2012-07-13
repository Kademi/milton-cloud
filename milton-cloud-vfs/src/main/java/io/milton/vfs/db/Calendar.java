/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a user calendar, which can contain events
 *
 * @author brad
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "owner"})}// item names must be unique within a directory
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Calendar implements Serializable {
    private List<CalEvent> events;

    private Long id;
    private BaseEntity owner;
    private String name;
    private String color;
    private Long ctag;
    private Date createdDate;
    private Date modifiedDate;

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

    @ManyToOne(optional=false)
    public BaseEntity getOwner() {
        return owner;
    }

    public void setOwner(BaseEntity owner) {
        this.owner = owner;
    }
       
    @Column
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Identifies the state of this calendar and all of its child resources
     * 
     * @return 
     */
    @Column(nullable = false)
    public Long getCtag() {
        return ctag;
    }

    public void setCtag(Long ctag) {
        this.ctag = ctag;
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
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @OneToMany(mappedBy = "calendar")
    public List<CalEvent> getEvents() {
        return events;
    }

    public void setEvents(List<CalEvent> events) {
        this.events = events;
    }
    
    
}
