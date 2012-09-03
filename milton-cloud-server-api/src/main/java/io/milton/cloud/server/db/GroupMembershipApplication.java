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

import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 * For closed groups, when a user applies for membership (ie by creating a new
 * account on the group's website) we will create a GroupMembershipApplication record
 * instead of a GroupMembership
 * 
 * This will prompt the admins to consider the request and accept or reject it
 *
 * @author brad
 */
@Entity
public class GroupMembershipApplication implements Serializable {
    private Long id;
    private Organisation withinOrg;
    private Profile member;
    private Group groupEntity;
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

    @ManyToOne(optional=false)    
    public Organisation getWithinOrg() {
        return withinOrg;
    }

    public void setWithinOrg(Organisation withinOrg) {
        this.withinOrg = withinOrg;
    }

    
    
    @ManyToOne(optional=false)
    public Profile getMember() {
        return member;
    }

    public void setMember(Profile member) {
        this.member = member;
    }

    @ManyToOne(optional=false)
    public Group getGroupEntity() {
        return groupEntity;
    }

    public void setGroupEntity(Group group) {
        this.groupEntity = group;
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
    
    
}
