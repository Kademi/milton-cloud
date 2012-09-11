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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

/**
 * An organisation contains users and websites. An organisation can have a
 * parent organisation which represents actual ownership, or administrative
 * control within the context of this system.
 *
 * For example, a business might have child organisations for its departments or
 * branches. Or a business might have child organisations for its customers,
 * meaning that the parent organsiation has administrative control of the
 * customers within this application.
 *
 * An organisation with no parent is the root organisation. It is only
 * permissible to have a single root organisation. This will generally be the
 * org which owns and operates the computer system
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name = "ORG_ENTITY", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"orgId"})}// website DNS names must be unique across whole system
)
@DiscriminatorValue("O")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Organisation extends BaseEntity implements VfsAcceptor {


    public static Organisation findRoot(Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(Restrictions.isNull("organisation"));
        return (Organisation) crit.uniqueResult();
    }

    public static Organisation findByOrgId(String orgId, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("orgId", orgId));
        return (Organisation) crit.uniqueResult();
    }

    public static List<Organisation> search(String q, Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        String s = q + "%";
        crit.add(Restrictions.ilike("title", s));
        // TODO: add other properties like address
        // TODO: impose parent org restriction
        return DbUtils.toList(crit, Organisation.class);
    }
    
    private String title;
    private String orgId; // globally unique; used for web addresses for this organisation
    private Organisation organisation;
    private List<Organisation> childOrgs;
    private List<Website> websites;
    private List<Group> groups;

    @Column(nullable = false)
    @Index(name = "idx_orgId")
    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @Column
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    
    
    @ManyToOne
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @OneToMany(mappedBy = "organisation")
    public List<Organisation> getChildOrgs() {
        return childOrgs;
    }

    public void setChildOrgs(List<Organisation> childOrgs) {
        this.childOrgs = childOrgs;
    }

    @OneToMany(mappedBy = "organisation")
    public List<Website> getWebsites() {
        return websites;
    }

    public void setWebsites(List<Website> websites) {
        this.websites = websites;
    }

    public List<Website> websites() {
        if (getWebsites() == null) {
            return Collections.EMPTY_LIST;
        } else {
            return getWebsites();
        }
    }

    public List<Organisation> childOrgs() {
        if (getChildOrgs() == null) {
            return Collections.EMPTY_LIST;
        } else {
            return getChildOrgs();
        }
    }

    public Organisation childOrg(String dn) {
        for (Organisation org : childOrgs()) {
            if (org.getName().equals(dn)) {
                return org;
            }
        }
        return null;
    }

    /**
     * Create a website for this organisation with the domain name given. Also
     * creates an alias subdomain if the alias argument is not null
     *
     */
    public Website createWebsite(String name, String dnsName, String theme, Profile user, Session session) {
        Repository r = createRepository(name, user, session);

        Website w = new Website();
        w.setOrganisation(this);
        w.setCreatedDate(new Date());
        w.setName(name);
        w.setDomainName(dnsName);
        w.setPublicTheme(theme);
        w.setInternalTheme(null);
        w.setCurrentBranch(Branch.TRUNK);
        w.setRepository(r);
        session.save(w);

        return w;
    }

    /**
     * Creates an organisation with an orgId the same as its name. Note that
     * orgId must be unique globally, so this might not always work
     *
     * @param orgName
     * @param session
     * @return
     */
    public Organisation createChildOrg(String orgName, Session session) {
        Organisation o = new Organisation();
        o.setOrganisation(this);
        o.setName(orgName);
        o.setOrgId(orgName);
        o.setCreatedDate(new Date());
        o.setModifiedDate(new Date());
        session.save(o);
        return o;
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Recursive method, goes up through parent orgs to find the group
     *
     * @param groupName
     * @param session
     * @return
     */
    public Group group(String groupName, Session session) {
        Group g = Group.findByOrgAndName(this, groupName, session);
        if (g == null) {
            Organisation parent = getOrganisation();
            if (parent != null) {
                g = parent.group(groupName, session);
            }
        }
        return g;
    }

    public List<Group> groups(Session session) {
        return Group.findByOrg(this, session);
    }

    @Override
    public void delete(Session session) {
        if (getWebsites() != null) {
            for (Website w : getWebsites()) {
                w.delete(session);
            }
        }
        setWebsites(null);
        if( getGroups() != null ) {
            for( Group g : getGroups() ) {
                g.delete(session);
            }
        }
        
        for( Organisation childOrg : childOrgs() ) {
            childOrg.delete(session);
        }
        session.delete(this);

    }

    @OneToMany(mappedBy = "organisation")
    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    /**
     * Check if this organisation is within, or is, the given org
     *
     * @param withinOrg
     * @return
     */
    public boolean isWithin(Organisation withinOrg) {
        if (withinOrg == null) {
            return false;
        }
        if (withinOrg == this) {
            return true;
        }
        Organisation parent = getOrganisation();
        if (parent != null) {
            return parent.isWithin(withinOrg);
        }
        return false;
    }
    
    /**
     * Return true if this entity contains or is the given user
     *
     * @param user
     * @return
     */
    public boolean containsUser(Profile p, Session session) {
        Subordinate s = Subordinate.find(this, p, session);
        return  s != null;
    }    
        
}
