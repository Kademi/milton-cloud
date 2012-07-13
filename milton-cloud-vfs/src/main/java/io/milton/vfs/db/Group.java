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

import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

/**
 * A user group, is a list of users and other groups. Is typically used to convey priviledges
 * to a selected set of users.
 * 
 * A group is defined within an organisation and can only convey privs within that
 * organisation, although that could be passed down to child organisations
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name="GROUP_ENTITY")
@DiscriminatorValue("G")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Group extends BaseEntity {
        
    public static String ADMINISTRATORS = "administrators";
    public static String USERS = "everyone";

    public static List<Group> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.add(Expression.eq("organisation", org));
        crit.addOrder(Order.asc("name"));
        return DbUtils.toList(crit, Group.class);
    }
    
    public static Group findByOrgAndName(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.add(Expression.eq("organisation", org));
        crit.add(Expression.eq("name", name));
        return (Group) crit.uniqueResult();
    }    
    
    private List<GroupMembership> members; // those entities in this group
    private List<GroupRole> groupRoles;
    
    public boolean isMember(BaseEntity u) {
        Criteria crit = SessionManager.session().createCriteria(GroupMembership.class);
        List list = crit.add(Expression.and(Expression.eq("member", u), Expression.eq("groupEntity", this))).list();
        boolean b = ( list != null && !list.isEmpty());
        return b;
    }

    @Override
    public boolean containsUser(BaseEntity entity) {
        return isMember(entity);
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    @OneToMany(mappedBy="groupEntity")
    public List<GroupMembership> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMembership> members) {
        this.members = members;
    }

    @OneToMany(mappedBy="grantee")
    public List<GroupRole> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRole> groupRoles) {
        this.groupRoles = groupRoles;
    }
    
    
    /**
     * Add or remove the role to this group. Updates the groupRoles list and
     * also saves the change in the session
     * 
     * @param roleName
     * @param isGrant
     * @param session 
     */
    public void grantRole(String roleName, boolean isGrant, Session session) {
        if( getGroupRoles() == null ) {
            setGroupRoles(new ArrayList<GroupRole>());
        }
        
        for (GroupRole gr : getGroupRoles()) {
            if (gr.getRoleName().equals(roleName)) {
                if( isGrant ) {
                    // nothing to do
                } else {
                    session.delete(gr);
                    getGroupRoles().remove(gr);
                }
                return;
            }
        }
        GroupRole gr = new GroupRole();
        gr.setGrantee(this);
        gr.setRoleName(roleName);
        session.save(gr);
        getGroupRoles().add(gr);
    }    
}
