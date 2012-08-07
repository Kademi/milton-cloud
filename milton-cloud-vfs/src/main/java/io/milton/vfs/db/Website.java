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
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Expression;

/**
 * A Website is an alias for a repository. The name of the website is the DNS
 * name
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name"})
}// website DNS names must be unique across whole system
)
public class Website implements Serializable, VfsAcceptor {

    public static List<Website> findByRepository(Repository repository, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.add(Expression.eq("repository", repository));
        return DbUtils.toList(crit, Website.class);
    }

    /**
     * Attempts to locate a website with the exact name give. Will follow alias
     * links
     *
     * @param name
     * @param session
     * @return
     */
    public static Website findByDomainName(String name, Session session) {
        Website w = findByName(name, session);
        while (w != null && w.getAliasTo() != null) {
            w = w.getAliasTo();
        }
        return w;
    }

    public static Website findByName(String name, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.add(Expression.eq("name", name));
        return DbUtils.unique(crit);
    }
    private Organisation organisation;
    private long id;
    private String name; // identifies the resource to webdav
    private Website aliasTo; // if not null, this website is really just an alias for that one
    private String redirectTo; // if not null, this website will redirect to that one
    private Repository repository;
    private String internalTheme;
    private String publicTheme;
    private String currentBranch;
    private Date createdDate;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(length = 255, nullable = false)
    @Index(name = "idx_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    /**
     * The internal theme is intended for logged in access
     *
     * @return
     */
    @Column
    public String getInternalTheme() {
        return internalTheme;
    }

    public void setInternalTheme(String internalTheme) {
        this.internalTheme = internalTheme;
    }

    /**
     * The public theme is intended for non-logged in access. It will usually
     * control the landing page and other content pages available to users prior
     * to signing up or logging in
     *
     * @return
     */
    @Column
    public String getPublicTheme() {
        return publicTheme;
    }

    public void setPublicTheme(String publicTheme) {
        this.publicTheme = publicTheme;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public Branch currentBranch() {
        if (repository.getBranches() == null) {
            return null;
        }
        for (Branch b : repository.getBranches()) {
            if (b.getName().equals(getCurrentBranch())) {
                return b;
            }
        }
        return null;
    }

    @ManyToOne(optional = false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @ManyToOne
    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
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
        }
        session.save(cur);
    }

    public void removeGroup(Group group, Session session) {
        GroupInWebsite cur = null;
        for (GroupInWebsite giw : GroupInWebsite.findByWebsite(this, session)) {
            if (giw.getWebsite() == this && giw.getUserGroup() == group) {
                session.delete(giw);
            }
        }
    }

    public List<GroupInWebsite> groups(Session session) {
        return GroupInWebsite.findByWebsite(this, session);
    }

    public void delete(Session session) {
        session.delete(this);
    }

    public Website createAlias(String aliasDnsName, Session session) {
        Website aliasWebsite = new Website();
        aliasWebsite.setOrganisation(getOrganisation());
        aliasWebsite.setCreatedDate(new Date());
        aliasWebsite.setName(aliasDnsName);
        aliasWebsite.setPublicTheme(null);
        aliasWebsite.setInternalTheme(null);
        aliasWebsite.setCurrentBranch(Branch.TRUNK);
        aliasWebsite.setAliasTo(this);
        session.save(aliasWebsite);
        return aliasWebsite;

    }
}
