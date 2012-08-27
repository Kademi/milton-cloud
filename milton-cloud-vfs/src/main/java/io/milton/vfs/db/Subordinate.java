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
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a Profile which is a subordinate to an organisation. This is 
 * when the profile has a group membership either directly related to the organisation
 * or to a subordinate organisation
 * 
 * This creates a link between the organisation and group membership which is subordinate
 * to that org
 * 
 * Note that the Subordinate record is created for every organisation that the
 * profile is subordinate to, so queries do not need to do recursive lookups
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Subordinate implements Serializable{
    private Long id;
    private Organisation withinOrg;
    private GroupMembership groupMembership;
    
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
    public GroupMembership getGroupMembership() {
        return groupMembership;
    }

    public void setGroupMembership(GroupMembership groupMembership) {
        this.groupMembership = groupMembership;
    }

    public void delete(Session session) {
        session.delete(session);
    }
    
    

    
    
}
