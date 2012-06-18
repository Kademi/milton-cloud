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

import io.milton.vfs.db.Repository;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 * An organisation contains users and websites. An organisation can have a parent
 * organisation which represents actual ownership, or administrative control within
 * the context of this system.
 * 
 * For example, a business might have child organisations for its departments or
 * branches. Or a business might have child organisations for its customers, meaning
 * that the parent organsiation has administrative control of the customers within
 * this application.
 * 
 * An organisation with no parent is the root organisation. It is only permissible
 * to have a single root organisation. This will generally be the org which owns
 * and operates the computer system
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name="ORG_ENTITY")
@DiscriminatorValue("O")
public class Organisation extends BaseEntity {

    public static Organisation findRoot(Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.add(Expression.isNull("organisation"));
        return (Organisation) crit.uniqueResult();
    }
    
    private List<BaseEntity> members;
    
    
        
    @OneToMany(mappedBy = "organisation")
    public List<BaseEntity> getMembers() {
        return members;
    }

    public void setMembers(List<BaseEntity> users) {
        this.members = users;
    }      


    public List<Website> websites() {
        List<Website> list = new ArrayList<>();
        if( getRepositories() != null ) {
            for( Repository r : getRepositories() ) {
                if( r instanceof Website ) {
                    list.add((Website)r);
                }
            }
        }
        return list;
    }

    /**
     * Find a unique name based on the given base name
     * 
     * @param nickName
     * @return 
     */
    public String findUniqueName(String nickName, Session session) {
        String candidateName = nickName;        
        int counter = 1;
        while(!isUniqueName(candidateName, session)) {
            candidateName = nickName + counter++;
        }
        return candidateName;
    }

    public boolean isUniqueName(String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Expression.and(Expression.eq("organisation", this), Expression.eq("name", name)));        
        return crit.uniqueResult() == null;
    }
    
    
    
}
