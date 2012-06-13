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
package io.milton.cloud.server.db;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import io.milton.cloud.server.db.Permission.DynamicPrincipal;
import io.milton.cloud.server.db.utils.SessionManager;
import io.milton.http.AccessControlledResource;

/**
 * A branch is a version of a repository which is mutable. Ie as changes are made
 * the version is updated.
 *
 * @author brad
 */
@Entity
public class Branch implements Serializable{
    
    /**
     * Special branch which always exists on a repository
     */
    public static String TRUNK = "trunk";
    
    private Long id;
    private String name;
    private Repository repository;
    private Commit head;
    private Branch linkedTo;
    private List<Permission> permissions; // can be granted permissions

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

        
    @ManyToOne(optional=true)
    public Commit getHead() {
        return head;
    }

    public void setHead(Commit head) {
        this.head = head;
    }
    
       
    /**
     * If set, then this repository is just a pointer to it
     * 
     * @return 
     */
    @ManyToOne
    public Branch getLinkedTo() {
        return linkedTo;
    }

    public void setLinkedTo(Branch linkedTo) {
        this.linkedTo = linkedTo;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @ManyToOne(optional=false)    
    public Repository getRepository() {
        return repository;
    }
    
    /**
     * Permissions which have been granted on this Branch
     * 
     * @return 
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "grantedOnBranch")
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> grantedPermissions) {
        this.permissions = grantedPermissions;
    }    
    
    public void grant(AccessControlledResource.Priviledge priviledge, DynamicPrincipal grantee) {
        if (isGranted(priviledge, grantee)) {
            return;
        }
        Permission p = new Permission();
        p.setGrantedOnBranch(this);
        p.setGranteePrincipal(grantee.name());
        p.setPriviledge(priviledge);
        SessionManager.session().save(p);
    }

    public boolean isGranted(AccessControlledResource.Priviledge priviledge, DynamicPrincipal grantee) {
        Session session = SessionManager.session();
        Criteria crit = session.createCriteria(Permission.class);
        crit.add(
                Expression.and(Expression.eq("granteePrincipal", grantee.name()), Expression.and(Expression.eq("grantedOnBranch", this), Expression.eq("priviledge", priviledge))));
        List list = crit.list();
        return list != null && !list.isEmpty();
    }    
        
}
