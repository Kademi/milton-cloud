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
import java.util.ArrayList;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents an organisation which is subordinate to another. This means that the
 * parent org is an owner, in an administrative sense, of the subordinate organisation
 * 
 * This link table is used for searching.
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SubOrg implements Serializable{
    
    /**
     * Removes and creates SubOrg records as necessary
     * 
     * @param childOrg 
     */
    public static void updateSubOrgs(Organisation childOrg, Session session) {
        if( childOrg.getParentOrgLinks() == null ) {
            childOrg.setParentOrgLinks(new ArrayList<SubOrg>());
        }
        for( SubOrg s : childOrg.getParentOrgLinks()) {
            session.delete(s);
        }
        // we will create a link from childOrg to itself
        // For the purpose of searching it is helpful for an org to be considered subordinate to itself
        Organisation parent = childOrg; 
        while( parent != null ) {
            SubOrg s = new SubOrg();
            s.setOwner(parent);
            s.setSuborg(childOrg);
            childOrg.getParentOrgLinks().add(s);
            session.save(s);
            parent = parent.getOrganisation();
        }
    }
        
    private Long id;
    private Organisation owner;
    private Organisation suborg;
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)    
    public Organisation getOwner() {
        return owner;
    }

    public void setOwner(Organisation owner) {
        this.owner = owner;
    }

    /**
     * This is the subordinate organisation, which is owned by the owner org
     * 
     * @return 
     */
    
    @ManyToOne(optional=false)
    public Organisation getSuborg() {
        return suborg;
    }

    public void setSuborg(Organisation suborg) {
        this.suborg = suborg;
    }

        
    public void delete(Session session) {
        session.delete(session);
    }
    
    

    
    
}
