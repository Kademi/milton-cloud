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
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OrgType implements Serializable {
    private List<Group> groups;

    public static OrgType findGroup(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(OrgType.class);
        crit.add(Restrictions.and(Restrictions.eq("organisation", org), Restrictions.eq("name", name)));
        return (OrgType) crit.uniqueResult();
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

    
}
