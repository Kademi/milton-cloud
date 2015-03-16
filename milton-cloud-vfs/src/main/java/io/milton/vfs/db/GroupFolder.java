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
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author dylan
 */
@javax.persistence.Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupFolder implements Serializable {

    private long id;
    private Organisation org;
    private String name;
    private String notes;
    
    public static GroupFolder get(long id, Session session){
        return (GroupFolder) session.get(GroupFolder.class, id);
    }
    
    public static GroupFolder create(Organisation org, String name, Session session){
        GroupFolder gf = new GroupFolder();
        gf.setName(name);
        gf.setOrg(org);
        session.save(gf);
        return gf;
    }

    public static List<GroupFolder> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(GroupFolder.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("org", org));
        crit.addOrder(Order.asc("name"));
        return DbUtils.toList(crit, GroupFolder.class);
    }

    public static GroupFolder findByOrgAndName(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(GroupFolder.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("org", org));
        crit.add(Restrictions.eq("name", name));
        crit.addOrder(Order.asc("name"));
        return (GroupFolder) crit.uniqueResult();
    }
    
    public static List<Group> findGroupsInFolderByName(Organisation org, String folderName, Session session){
        GroupFolder groupFolder = findByOrgAndName(org, folderName, session);
        return findGroupsInFolder(org, groupFolder, session);
    }

    public static List<Group> findGroupsInFolder(Organisation org, GroupFolder groupFolder, Session session) {
        Criteria crit = session.createCriteria(Group.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", org));
        crit.add(Restrictions.eq("groupFolder", groupFolder));
        crit.add(Restrictions.isNull("deleted"));
        crit.addOrder(Order.asc("name"));
        return DbUtils.toList(crit, Group.class);
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Organisation getOrg() {
        return org;
    }

    public void setOrg(Organisation org) {
        this.org = org;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Column(length = 3000)
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void delete(Session session) {
        List<Group> groups = findGroupsInFolder(this.org, this, session);
        groups.stream().forEach((group) -> {
            group.setGroupFolder(null);
            session.save(group);
        });
        session.delete(this);
    }
}
