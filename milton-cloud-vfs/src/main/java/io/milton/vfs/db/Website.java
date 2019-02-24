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
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * A Website is a repository which can be accessed via a DNS name. The DNS name
 * can be specified explicitly or it can be the name + primary domain name
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"domainName"}),}// website DNS names must be unique across whole system
)
@DiscriminatorValue("W")
public class Website extends Repository implements VfsAcceptor {

    public static String REPO_TYPE_WEBSITE = "W"; // discriminator


    /**
     * Attempts to locate a website with the exact name give. Will follow alias
     * links
     *
     * @param name
     * @param session
     * @return
     */
    public static Website findByDomainName(String name, Session session) {
        Website w = findByDomainNameDirect(name, session);
        while (w != null && w.getAliasTo() != null) {
            w = w.getAliasTo();
        }
        return w;
    }

    public static Website findByDomainNameDirect(String domainName, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("domainName", domainName));
        Website w = DbUtils.unique(crit);
        return w;
    }

    public static Website findByName(String name, Session session) {
        Website w = findByNameDirect(name, session);
        while (w != null && w.getAliasTo() != null) {
            w = w.getAliasTo();
        }
        return w;
    }

    public static Website findByNameDirect(String name, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("name", name));
        return DbUtils.unique(crit);
    }

    /**
     * Checks for null deleted flag
     *
     * @param org
     * @param session
     * @return
     */
    public static List<Website> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", org));
        crit.add(Restrictions.isNull("deleted"));
        crit.addOrder(Order.asc("name"));
        return DbUtils.toList(crit, Website.class);
    }

    public static Website get(Session session, Long themeSiteId) {
        return (Website) session.get(Website.class, themeSiteId);
    }

    private Organisation organisation; // will generally be same as baseEntity on underlying repo
    private String domainName; // identifies the resource to webdav. This is the DNS name, eg www.bradsite.com
    private Website aliasTo; // if not null, this website is really just an alias for that one
    private String redirectTo; // if not null, this website will redirect to that one
    private String mailServer; // if not null, will be used for email sending and generating MX records
    private String dkimSelector;
    private String dkimPrivateKey;
    private String imageHash; // hash of a thumbnail for this site

    @Override
    @Transient
    public String getRepoType() {
        return REPO_TYPE_WEBSITE;
    }

    @Column(length = 255, nullable = true)
    @Index(name = "idx_domain_name")
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String name) {
        this.domainName = name;
    }

    @ManyToOne(optional = true) // must allow null when using single table inheritance
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    /**
     * If set, this website is really just an alias for that one. This means
     * that users will see exactly the same thing on both. Useful for having a
     * "local" domain name as well as an externally delegated one.
     *
     * @return
     */
    @ManyToOne
    public Website getAliasTo() {
        return aliasTo;
    }

    public void setAliasTo(Website aliasTo) {
        this.aliasTo = aliasTo;
    }

    /**
     * If set any requests to this website will redirect to the one specified
     * here
     *
     * @return
     */
    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(String redirectTo) {
        this.redirectTo = redirectTo;
    }

    @Column(length = 2000)
    public String getDkimPrivateKey() {
        return dkimPrivateKey;
    }

    public void setDkimPrivateKey(String dkimPrivateKey) {
        this.dkimPrivateKey = dkimPrivateKey;
    }

    public String getDkimSelector() {
        return dkimSelector;
    }

    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }


    public String getMailServer() {
        return mailServer;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public void addGroup(Group group, Session session) {
        GroupInWebsite cur = null;
        for (GroupInWebsite giw : GroupInWebsite.findByWebsite(this, session)) {
            if (giw.getWebsite() == this && giw.getUserGroup() == group) {
                cur = giw;
                break;
            }
        }
        if (cur == null) {
            cur = new GroupInWebsite();
            cur.setWebsite(this);
            cur.setUserGroup(group);
        } else {
            // already in group
        }
        session.save(cur);
    }

    public void removeGroup(Group group, Session session) {
        for (GroupInWebsite giw : GroupInWebsite.findByWebsite(this, session)) {
            if (giw.getWebsite() == this && giw.getUserGroup() == group) {
                session.delete(giw);
            }
        }
    }

    public List<GroupInWebsite> groups(Session session) {
        return GroupInWebsite.findByWebsite(this, session);
    }

    public boolean hasGroup(Group g, Session session) {
        for( GroupInWebsite giw : groups(session)) {
            if( g == giw.getUserGroup()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void delete(Session session) {
        super.delete(session);
    }

    public Website createAlias(String aliasDnsName, Session session) {
        Website aliasWebsite = new Website();
        aliasWebsite.setBaseEntity(getBaseEntity());
        aliasWebsite.setCreatedDate(new Date());
        aliasWebsite.setDomainName(aliasDnsName);
        aliasWebsite.setLiveBranch(Branch.TRUNK);
        aliasWebsite.setAliasTo(this);
        session.save(aliasWebsite);
        return aliasWebsite;

    }

    @Override
    public void softDelete(Session session) {
        String deletedName = Organisation.getDeletedName(getName()); // change name to avoid name conflicts with new resources
        this.setName(deletedName);
        this.setDeleted(true);
        this.setDomainName(null);
        session.save(this);
    }

    /**
     * Can only move to organisations
     *
     * @param dest
     * @param session
     */
    @Override
    public void moveTo(BaseEntity dest,Profile movedBy, Session session) {
        getOrganisation().getWebsites().remove(this);
        Organisation destOrg = (Organisation) dest;
        this.setOrganisation(destOrg);
        destOrg.getWebsites().add(this);
        super.moveTo(dest, movedBy, session);
        session.save(getOrganisation());
    }


}
