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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private List<OrgType> orgTypes;
    private static final Logger log = LoggerFactory.getLogger(Organisation.class);

    public static String getDeletedName(String origName) {
        return origName + "-deleted-" + System.currentTimeMillis();
    }

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

    public static List<Organisation> findByOrgType(OrgType orgType, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("orgType", orgType));
        return DbUtils.toList(crit, Organisation.class);
    }

    public static List<Organisation> search(String q, Organisation organisation, OrgType orgType, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        crit.add(notDeleted);
        String s = q + "%";
        Disjunction or = Restrictions.disjunction();
        or.add(Restrictions.ilike("title", s));
        or.add(Restrictions.ilike("orgId", s));
        crit.add(or);
        if( orgType != null ) {
            crit.add(Restrictions.eq("orgType", orgType));
        }
        // TODO: add other properties like address
        Criteria critParentLink = crit.createCriteria("parentOrgLinks");
        critParentLink.add(Restrictions.eq("owner", organisation));
        return DbUtils.toList(crit, Organisation.class);
    }

    public static Organisation getOrganisation(String name, Session session) {
        return (Organisation) session.get(Organisation.class, name);
    }

    public static Organisation getRootOrg(Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(Restrictions.isNull("organisation"));
        Organisation org = (Organisation) crit.uniqueResult();
        return org;
    }

    public static Organisation getOrganisation(Organisation organisation, String name, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(
                Restrictions.and(
                Restrictions.eq("organisation", organisation),
                Restrictions.eq("name", name)));
        Organisation org = (Organisation) crit.uniqueResult();
        return org;
    }
    private String title;
    private String orgId; // globally unique; used for web addresses for this organisation
    private String phone;
    private String address;
    private String addressLine2;
    private String state;
    private String postcode;
    private OrgType orgType; // nullable, if present has the type of the org which can be used on signup form
    private Organisation organisation; // the parent org
    private Boolean deleted;
    private List<Organisation> childOrgs;
    private List<Website> websites;
    private List<Group> groups;
    private List<SubOrg> parentOrgLinks;

    @Transient
    public String getFormattedName() {
        if (getTitle() != null && getTitle().length() > 0) {
            return getTitle();
        }
        return getOrgId();
    }

    @Column(nullable = false)
    @Index(name = "idx_orgId")
    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @OneToMany(mappedBy = "organisation")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    @Column
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Column
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Column
    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    @Column
    public String getAddressState() {
        return state;
    }

    public void setAddressState(String state) {
        this.state = state;
    }

    @Column
    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    @ManyToOne
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    /**
     * Used for soft deletions. When this flag is set to true the organisation
     * will generally not be shown. As part of soft deleting, any unique values
     * on this organisation or logically contained within it must be set to
     * values not likely to be used
     *
     * @return
     */
    @Column
    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Updates the organisation reference and also any SubOrg links
     *
     * @param newParent
     * @param session
     */
    public void setOrganisation(Organisation newParent, Session session) {
        log.info("setOrganisation: this=" + getOrgId());
        Organisation oldParent = this.getOrganisation();
        if (oldParent != null) {
            oldParent.getChildOrgs().remove(this);
        }
        if (newParent != null) {
            if (newParent.getChildOrgs() == null) {
                newParent.setChildOrgs(new ArrayList<Organisation>());
            }
            newParent.getChildOrgs().add(this);
        }

        this.organisation = newParent;

        if (oldParent != null && oldParent != newParent) {
            log.info("update subs");
            SubOrg.updateSubOrgs(this, SessionManager.session());
        }
        if (oldParent != null) {
            session.save(oldParent);
        }
        if (newParent != null) {
            session.save(newParent);
        }
        session.save(this);
        log.info("finished move");
    }

    @OneToMany(mappedBy = "organisation")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Organisation> getChildOrgs() {
        return childOrgs;
    }

    public void setChildOrgs(List<Organisation> childOrgs) {
        this.childOrgs = childOrgs;
    }

    @OneToMany(mappedBy = "organisation")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Website> getWebsites() {
        return websites;
    }

    public void setWebsites(List<Website> websites) {
        this.websites = websites;
    }

    @OneToMany(mappedBy = "suborg")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<SubOrg> getParentOrgLinks() {
        return parentOrgLinks;
    }

    public void setParentOrgLinks(List<SubOrg> parentOrgLinks) {
        this.parentOrgLinks = parentOrgLinks;
    }

    @ManyToOne
    public OrgType getOrgType() {
        return orgType;
    }

    public void setOrgType(OrgType orgType) {
        this.orgType = orgType;
    }

    public List<Website> websites() {
        if (getWebsites() == null) {
            return Collections.EMPTY_LIST;
        } else {
            List<Website> list = new ArrayList<>();
            for (Website w : getWebsites()) {
                if (!w.deleted()) {
                    list.add(w);
                }
            }
            return list;
        }
    }

    public Website website(String name) {
        for (Website w : websites()) {
            if (w.getName().equals(name)) {
                return w;
            }
        }
        return null;
    }

    public List<Organisation> childOrgs() {
        if (getChildOrgs() == null) {
            return Collections.EMPTY_LIST;
        } else {
            List<Organisation> list = new ArrayList<>();
            for (Organisation o : getChildOrgs()) {
                if (!o.deleted()) {
                    list.add(o);
                }
            }
            return list;
        }
    }

    public Organisation childOrg(String orgId) {
        for (Organisation org : childOrgs()) {
            if (org.getOrgId().equals(orgId)) {
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
        Website w = new Website();
        w.setBaseEntity(this);
        w.setOrganisation(this);
        w.setCreatedDate(new Date());
        w.setName(name);
        w.setDomainName(dnsName);
        w.setLiveBranch(Branch.TRUNK);
        if (this.getWebsites() == null) {
            this.setWebsites(new ArrayList<Website>());
        }
        this.getWebsites().add(w);
        session.save(w);

        Branch b = w.createBranch(Branch.TRUNK, user, session);
        b.setPublicTheme(theme);
        b.setInternalTheme(null);

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
        return createChildOrg(orgName, orgName, session);
    }

    public Organisation createChildOrg(String orgId, String title, Session session) {
        Organisation o = new Organisation();
        o.setOrganisation(this);
        o.setOrgId(orgId);
        o.setTitle(title);
        o.setCreatedDate(new Date());
        o.setModifiedDate(new Date());
        if (this.getChildOrgs() == null) {
            this.setChildOrgs(new ArrayList<Organisation>());
        }
        this.getChildOrgs().add(o);
        session.save(o);
        SubOrg.updateSubOrgs(o, session);
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
        // Delete links from this to each parent
        if (getParentOrgLinks() != null) {
            for (SubOrg so : getParentOrgLinks()) {
                session.delete(so);
            }
            setParentOrgLinks(null);
        }
        if (getWebsites() != null) {
            for (Website w : getWebsites()) {
                w.delete(session);
            }
        }
        setWebsites(null);
        if (getGroups() != null) {
            for (Group g : getGroups()) {
                g.delete(session);
            }
        }

        for (Organisation childOrg : childOrgs()) {
            childOrg.delete(session);
        }
        session.delete(this);
        Organisation parent = getOrganisation();
        if (parent != null) {
            parent.getChildOrgs().remove(this);
            session.save(parent);
        }
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
        return s != null;
    }

    /**
     * Creates and adds to this Org's groups, but does not save
     *
     * @param newName
     * @return
     */
    public Group createGroup(String newName) {
        Group g = new Group();
        g.setName(newName);
        g.setOrganisation(this);
        g.setCreatedDate(new Date());
        g.setModifiedDate(new Date());
        if (this.getGroups() == null) {
            this.setGroups(new ArrayList<Group>());
        }
        getGroups().add(g);
        return g;
    }

    public void softDelete(Session session) {
        this.setDeleted(true);
        this.setOrgId(getDeletedName(orgId));
        session.save(this);

        if (this.getWebsites() != null) {
            for (Website w : getWebsites()) {
                w.softDelete(session);
            }
        }
    }

    /**
     * null-safe alias for getDeleted
     *
     * @return
     */
    public boolean deleted() {
        return getDeleted() != null && getDeleted();
    }

    public OrgType createOrgType(String name, Session session) {
        OrgType existing = orgType(name);
        if (existing != null) {
            throw new RuntimeException("An organisation type with that name already exists");
        }
        Organisation parent = getOrganisation();
        while (parent != null) {
            if (parent.orgType(name) != null) {
                throw new RuntimeException("An organisation type with that name exists in a parent organisation: " + parent.getOrgId());
            }
            parent = parent.getOrganisation();
        }
        OrgType ot = new OrgType();
        ot.setName(name);
        ot.setDisplayName(name);
        ot.setOrganisation(this);
        getOrgTypes().add(ot);
        session.save(this);
        session.save(ot);
        return ot;
    }

    public OrgType orgType(String name, boolean autoCreate, Session session) {
        OrgType ot = orgType(name);
        if (ot == null) {
            if (autoCreate) {
                if (getOrgTypes() == null) {
                    setOrgTypes(new ArrayList<OrgType>());
                }
                ot = new OrgType();
                ot.setName(name);
                ot.setDisplayName(name);
                ot.setOrganisation(this);
                getOrgTypes().add(ot);
                session.save(this);
                session.save(ot);
            }
        }
        return ot;
    }

    public OrgType orgType(String name) {
        if (getOrgTypes() != null) {
            for (OrgType ot : getOrgTypes()) {
                if (ot.getName().equals(name)) {
                    return ot;
                }
            }
        }
        return null;
    }

    @OneToMany(mappedBy = "organisation")
    public List<OrgType> getOrgTypes() {
        return orgTypes;
    }

    public void setOrgTypes(List<OrgType> orgTypes) {
        this.orgTypes = orgTypes;
    }

    /**
     * Get all linked memberships. Uses SessionManager.session
     *
     * @return
     */
    @Transient
    public List<GroupMembership> getMembers() {
        return GroupMembership.find(this, SessionManager.session());
    }
}
