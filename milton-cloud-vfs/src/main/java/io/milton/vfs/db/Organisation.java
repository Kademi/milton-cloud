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
import org.hibernate.criterion.Conjunction;
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
    @UniqueConstraint(columnNames = {"adminDomain"})}// website DNS names must be unique across whole system
        )
@DiscriminatorValue("O")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Organisation extends BaseEntity implements VfsAcceptor {

    private List<OrgType> orgTypes;
    private static final Logger log = LoggerFactory.getLogger(Organisation.class);

    public static String getDeletedName(String origName) {
        return origName + "-deleted-" + System.currentTimeMillis();
    }


    /**
     * Find the given orgId within an administrative org
     *
     * @param adminOrg
     * @param orgId
     * @param session
     * @return
     */
    public static Organisation findByOrgId(Organisation adminOrg, String orgId, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        Criteria critSubOrg = crit.createCriteria("parentOrgLinks");

        crit.setCacheable(true);
        crit.add(Restrictions.eq("orgId", orgId));
        critSubOrg.add(Restrictions.eq("owner", adminOrg));
        return (Organisation) crit.uniqueResult();
    }

    public static Organisation get(Long id, Session session) {
        return (Organisation) session.get(Organisation.class, id);
    }

    public static Organisation findByAdminDomain(String adminDomain, Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("adminDomain", adminDomain));
        return DbUtils.unique(crit);
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

        String[] arr = q.split(" ");
        Conjunction con = Restrictions.conjunction();
        for (String queryPart : arr) {
            Disjunction dis = Restrictions.disjunction();
            String s = "%" + queryPart + "%";
            dis.add(Restrictions.ilike("title", s));
            dis.add(Restrictions.ilike("orgId", s));
            dis.add(Restrictions.ilike("address", s));
            dis.add(Restrictions.ilike("addressLine2", s));
            dis.add(Restrictions.ilike("postcode", s));
            con.add(dis);
        }
        crit.add(con);

        if (orgType != null) {
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
        List<Organisation> list = DbUtils.toList(crit, Organisation.class);
        if( !list.isEmpty()) {
            return list.get(0);
        }
        return null;
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
    private String orgId; // locally unique, ie within the administrative domain
    private String adminDomain; // globally unique, if not null. Used for admin console address
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

    public Organisation() {

    }

    
    
    @Transient
    @Override
    public String getFormattedName() {
        if (getTitle() != null && getTitle().length() > 0) {
            return getTitle();
        }
        if (getAdminDomain() != null) {
            return getAdminDomain();
        }
        return getOrgId();
    }

    /**
     * Is required to be unique within its administrative domain, even in the
     * presence or a nested hierarchy. If not set by the user, will be set to
     * the ID property
     *
     * So it might be null on initial save, but will then be immediately
     * populated
     *
     * @return
     */
    @Column(nullable = true)
    @Index(name = "idx_orgId")
    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @Column(nullable = true)
    @Index(name = "idx_adminDomain")
    public String getAdminDomain() {
        return adminDomain;
    }

    public void setAdminDomain(String adminDomain) {
        if (adminDomain != null) {
            if (!adminDomain.equals(adminDomain.toLowerCase())) {
                throw new RuntimeException("Admin domain must be all lower case");
            }
        }
        this.adminDomain = adminDomain;
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

    public Organisation childOrg(String orgId, Session session) {
        return findByOrgId(this, orgId, session);
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
        if (orgId != null) {
            orgId = orgId.trim();
            if (orgId.length() == 0) {
                orgId = null;
            }
        }
        o.setOrgId(orgId);
        o.setTitle(title);
        o.setCreatedDate(new Date());
        o.setModifiedDate(new Date());
        if (this.getChildOrgs() == null) {
            this.setChildOrgs(new ArrayList<Organisation>());
        }
        this.getChildOrgs().add(o);
        session.save(o);
        if (o.getOrgId() == null || o.getOrgId().trim().length() == 0) {
            findUniqueOrgId(o);
            session.save(o);
        }
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

        // TODO: problem with soft deleting is that memberships will still be linked
        // to parent org via subordinate, so will appear in manage users.
        // should remove them, but need to check there isnt another membership
        // from a non-deleted org which implies the subordinate record
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

    @Transient
    public String getTitleOrId() {
        if (title != null) {
            return title;
        }
        return getOrgId();
    }

    /**
     * Remove all memberships of this profile within this organiosation or
     * subordinate organisations
     *
     * @param profile
     */
    public void removeMember(Profile profile, Session session) {
        log.info("removeMember: profileid=" + profile.getId() + " org=" + getOrgId());
        List<GroupMembership> toDelete = new ArrayList<>();
        if (profile.getMemberships() != null) {
            for (GroupMembership m : profile.getMemberships()) {
                Organisation memberWithin = m.getWithinOrg();
                if (memberWithin.isWithin(this)) {
                    log.info("Remove membership of user: " + profile.getName() + " from org: " + memberWithin.getOrgId());
                    toDelete.add(m);
                }
            }
        }
        for (GroupMembership m : toDelete) {
            m.delete(session);
        }
    }

    public Organisation childOrg(Long orgId, Session session) {
        Organisation child = get(orgId, session);
        if (child == null) {
            return null;
        }
        if (child.isWithin(this)) {
            return child;
        }
        return null;
    }

    private void findUniqueOrgId(Organisation o) {
        o.setOrgId(System.currentTimeMillis() + ""); // hack, todo, check for uniqueness within the account
    }
    
    /**
     * Check if the organisation ID on this org is unique within its administrative domain (ie first
     * parent org with a non-null adminDomain)
     * 
     * @return 
     */
    public boolean isOrgIdUniqueWithinAdmin(Session session) {
        Organisation admin = closestAdminOrg();
        Organisation withSameOrgId = Organisation.findByOrgId(admin, getOrgId(), session);
        if( withSameOrgId == null || withSameOrgId.getId() == this.getId()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Find the closest parent organisation with a non-null admin domain, or
     * this org if it has a non-null admin domain
     * 
     * @return 
     */
    public Organisation closestAdminOrg() {
        Organisation p = this;
        while( p.getAdminDomain() == null && p.getOrganisation() != null ) {
            p = p.getOrganisation();
        }
        return p;
    }
}
