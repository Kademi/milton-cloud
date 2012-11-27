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
import java.io.Serializable;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

/**
 * A type of organisation, which is used to limit what organisations a group is
 * permitted to signup to. For example, the group "pharmacist" might only
 * be permitted to signup to organisations of type "pharmacy"
 *
 * @author brad
 */
@javax.persistence.Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OrgType implements Serializable {
    private List<Group> groups;

    public static OrgType find(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(OrgType.class);
        crit.add(Restrictions.and(Restrictions.eq("organisation", org), Restrictions.eq("name", name)));
        return DbUtils.unique(crit);
    }    
    
    public static List<OrgType> findAllForOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(OrgType.class);
        crit.add(Restrictions.eq("organisation", org));
        return DbUtils.toList(crit, OrgType.class);
    }       
    
    private long id;
    private String name; // matched on orgs upload spreadsheet
    private String displayName; // used on the registration form
    private Organisation organisation; // the org which owns the list of org types

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    @Column(nullable = false)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @OneToMany(mappedBy = "regoOrgType")
    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    /**
     * Removes this org type from any organisations which are associated with it,
     * then removed this from the parent org type list and then finally deletes this
     * 
     * @param session 
     */
    public void delete(Session session) {
        for( Organisation org : Organisation.findByOrgType(this, session)) {
            org.setOrgType(null);
            session.save(org);
        }
        getOrganisation().getOrgTypes().remove(this);
        session.delete(this);
    }

    
}
