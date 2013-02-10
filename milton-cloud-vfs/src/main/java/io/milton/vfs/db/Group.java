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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * A user group, is a list of users and other groups. Is typically used to
 * convey priviledges to a selected set of users.
 *
 * A group is defined within an organisation and can only convey privs within
 * that organisation, although that could be passed down to child organisations
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name = "GROUP_ENTITY")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Group implements Serializable, VfsAcceptor {

    public static Group findGroup(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.add(Restrictions.and(Restrictions.eq("organisation", org), Restrictions.eq("name", name)));
        return (Group) crit.uniqueResult();
    }    
    
    public static String ADMINISTRATORS = "administrators";
    public static String USERS = "everyone";
    /**
     * Intended to be used as the public group in SpliffySecurityManager
     */
    public static String PUBLIC = "public";
    public static String REGO_MODE_OPEN = "o";
    public static String REGO_MODE_ADMIN_REVIEW = "a";
    public static String REGO_MODE_CLOSED = "c";

    public static List<Group> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", org));
        crit.addOrder(Order.asc("name"));
        return DbUtils.toList(crit, Group.class);
    }

    public static Group findByOrgAndName(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", org));
        crit.add(Restrictions.eq("name", name));
        return (Group) crit.uniqueResult();
    }
    private long id;
    private String name;
    private Date createdDate;
    private Date modifiedDate;
    private Organisation organisation;
    private List<GroupMembership> groupMemberships; // those entities in this group
    private List<GroupRole> groupRoles;
    private String registrationMode; // whether to allow anyone to join this group    
    private OrgType regoOrgType; // label to display to users in signup form to selet their organisation
    private Organisation rootRegoOrg; // root org to select from when users select an org

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @ManyToOne(optional = false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    @OneToMany(mappedBy = "groupEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<GroupMembership> getGroupMemberships() {
        return groupMemberships;
    }

    public void setGroupMemberships(List<GroupMembership> members) {
        this.groupMemberships = members;
    }

    @OneToMany(mappedBy = "grantee")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<GroupRole> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<GroupRole> groupRoles) {
        this.groupRoles = groupRoles;
    }

    /**
     * Allowable registration option: - "o" = open, anyone can register and be
     * immediately active - "c" = closed, no self registration - "a" =
     * administrator enabled, anyone can register but their account only becomes
     * active after being enabled
     *
     * @return
     */
    @Column(nullable = false)
    public String getRegistrationMode() {
        return registrationMode;
    }

    /**
     * Allowable registration option: - "o" = open, anyone can register and be
     * immediately active - "c" = closed, no self registration - "a" =
     * administrator enabled, anyone can register but their account only becomes
     * active after being enabled
     *
     * @return
     */
    public void setRegistrationMode(String registrationMode) {
        this.registrationMode = registrationMode;
    }

    @ManyToOne
    public OrgType getRegoOrgType() {
        return regoOrgType;
    }

    public void setRegoOrgType(OrgType regoOrgType) {
        this.regoOrgType = regoOrgType;
    }

    

    @ManyToOne(optional=true)
    public Organisation getRootRegoOrg() {
        return rootRegoOrg;
    }

    public void setRootRegoOrg(Organisation rootRegoOrg) {
        this.rootRegoOrg = rootRegoOrg;
    }

    
    
    /**
     * Add or remove the role to this group. Updates the groupRoles list and
     * also saves the change in the session
     *
     * @param roleName
     * @param isGrant - true means to grant permission, false to remove
     * @param session
     */
    public GroupRole grantRole(String roleName, Session session) {
        return grantRole(null, null, roleName, session);
    }

    public GroupRole grantRole(Repository repository, String roleName, Session session) {
        return grantRole(repository, null, roleName, session);
    }

    public GroupRole grantRole(Organisation org, String roleName, Session session) {
        return grantRole(null, org, roleName, session);
    }

    private GroupRole grantRole(Repository repo, Organisation withinOrg, String roleName, Session session) {
        if (getGroupRoles() == null) {
            setGroupRoles(new ArrayList<GroupRole>());
        }

        // Check if this role already exists
        for (GroupRole gr : getGroupRoles()) {
            if (gr.getRoleName().equals(roleName)) {
                if (gr.getWithinOrg() != null && gr.getWithinOrg() == withinOrg) {
                    // nothing to do
                    return null;
                } else if (gr.getRepository() != null && gr.getRepository() == repo) {
                    // nothing to do
                    return null;
                } else if( withinOrg == null && repo == null && gr.getRepository() == null && gr.getWithinOrg() == null) {
                    return null;
                }
            }
        }
        GroupRole gr = new GroupRole();
        gr.setGrantee(this);
        gr.setRoleName(roleName);
        gr.setRepository(repo);
        gr.setWithinOrg(withinOrg);
        session.save(gr);
        getGroupRoles().add(gr);
        return gr;
    }

    public boolean hasRole(String roleName) {
        if (getGroupRoles() == null) {
            return false;
        }
        for (GroupRole r : getGroupRoles()) {
            if (r.getRoleName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMember(BaseEntity u, Organisation withinOrg, Session session) {
        Criteria crit = session.createCriteria(GroupMembership.class);
        crit.add(Restrictions.eq("member", u));
        crit.add(Restrictions.eq("groupEntity", this));
        crit.add(Restrictions.eq("withinOrg", withinOrg));
        boolean b = !DbUtils.toList(crit, GroupMembership.class).isEmpty();
        return b;
    }

    public boolean containsUser(BaseEntity entity, Organisation withinOrg, Session session) {
        return isMember(entity, withinOrg, session);
    }

    /**
     * Is the given entity a member of this group, regardless of organisation
     *
     * @param entity
     * @return
     */
    public boolean isMember(BaseEntity entity) {
        Criteria crit = SessionManager.session().createCriteria(GroupMembership.class);
        crit.add(Restrictions.eq("member", entity));
        crit.add(Restrictions.eq("groupEntity", this));
        boolean b = !DbUtils.toList(crit, GroupMembership.class).isEmpty();
        return b;
    }

    public void delete(Session session) {
        if (getGroupRoles() != null) {
            for (GroupRole gr : getGroupRoles()) {
                gr.delete(session);
            }
        }
        if (getGroupMemberships() != null) {
            List<GroupMembership> list = new ArrayList<>(getGroupMemberships());
            for (GroupMembership gm : list) {
                gm.delete(session);
                session.flush();
            }
        }
        for (GroupInWebsite giw : GroupInWebsite.findByGroup(this, session)) {
            session.delete(giw);
        }
        session.delete(this);
    }

    /**
     * Sets the new org type on this group, updating collections in the old and
     * new org type objects if they're not null
     * 
     * @param newOrgType
     * @param session 
     */
    public void setRegoOrgType(OrgType newOrgType, Session session) {
        OrgType oldOrgType = getRegoOrgType();
        if( oldOrgType != null ) {
            oldOrgType.getGroups().remove(this);
            session.save(oldOrgType);
        }
        
        setRegoOrgType(newOrgType);
        
        if( newOrgType != null ) {
            if( newOrgType.getGroups() == null ) {
                newOrgType.setGroups(new ArrayList<Group>());
            }
            newOrgType.getGroups().add(this);
        }
    }

    @Transient
    public long getNumMembers() {
        return GroupMembership.count(this, SessionManager.session());
    }
}
