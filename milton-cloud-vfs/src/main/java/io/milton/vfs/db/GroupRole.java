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
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A GroupRole is a well known idenfifier for a set of permissions. It is assigned
 * to a Group to give that group the permissions which the role conveys.
 * 
 * For example the "Administrator" role will have a very broad set of permissions,
 * while "Content Author" will have a reduced set.
 * 
 * The actual permissions will depend on application configuration.
 * 
 * 
 * Note: when referring here to "the principal" we are talking about the user
 * which is having its permissions calculated, and is a member of a group with a grouprole
 *
 * A GroupRole can be attached to a protected resource in one of three ways
 *  - if a repository is specified then that is what the permissions apply to
 *  - otherwise, if an organsiation is specified that that is the applicable object
 *  - otherwise, ie if nothing is explicitly stated, then the applicable organisation
 * is inferred from the user's group membership. Ie whatever organisation their
 * membership is on
 * 
 * @author brad
 */
@Entity
@Table(
//uniqueConstraints = {
//    @UniqueConstraint(columnNames = {"roleName", "grantee","within_org","repository"})}
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupRole implements Serializable{
    
    private long id;
    private String roleName;
    private Group grantee;
    private Organisation withinOrg; // optional. If null the withinorg is the org of the group membership which has this role
    private Repository repository; // optional


    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @ManyToOne(optional=false)
    public Group getGrantee() {
        return grantee;
    }

    public void setGrantee(Group grantee) {
        this.grantee = grantee;
    }

    /**
     * Optional. Where this is present it means that the the role only applies
     * within this given organisation. If not present it means that the role
     * applies within the organisation associated with the group membership
     * 
     * @return 
     */
    @ManyToOne
    public Organisation getWithinOrg() {
        return withinOrg;
    }

    public void setWithinOrg(Organisation withinOrg) {
        this.withinOrg = withinOrg;
    }

    @ManyToOne
    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
            
    

    public void delete(Session session) {
        session.delete(this);
    }

    
    
}
