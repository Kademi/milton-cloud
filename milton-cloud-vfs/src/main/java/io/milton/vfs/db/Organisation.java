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

import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.criterion.Expression;

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
@Table(name = "ORG_ENTITY")
@DiscriminatorValue("O")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Organisation extends BaseEntity implements VfsAcceptor {

    public static Organisation findRoot(Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.add(Expression.isNull("organisation"));
        return (Organisation) crit.uniqueResult();
    }
    
    private List<BaseEntity> members;
    private List<Website> websites;

    @OneToMany(mappedBy = "organisation")
    public List<BaseEntity> getMembers() {
        return members;
    }

    public void setMembers(List<BaseEntity> users) {
        this.members = users;
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

    /**
     * Find a unique name based on the given base name
     *
     * @param nickName
     * @return
     */
    public String findUniqueName(String nickName, Session session) {
        String candidateName = nickName;
        int counter = 1;
        while (!isUniqueName(candidateName, session)) {
            candidateName = nickName + counter++;
        }
        return candidateName;
    }

    public boolean isUniqueName(String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Expression.and(Expression.eq("organisation", this), Expression.eq("name", name)));
        return crit.uniqueResult() == null;
    }

    public List<Organisation> childOrgs() {
        List<Organisation> list = new ArrayList<>();
        if (this.getMembers() != null) {
            for (BaseEntity be : getMembers()) {
                if (be instanceof Organisation) {
                    list.add((Organisation) be);
                }
            }
        }
        return list;
    }

    public BaseEntity childOrg(String dn) {
        if (this.getMembers() != null) {
            for (BaseEntity be : this.getMembers()) {
                if (be.getName().equals(dn)) {
                    return be;
                }
            }
        }
        return null;
    }

    /**
     * Create a website for this organisation with the domain name given. Also
     * creates an alias subdomain if the alias argument is not null
     *
     */
    public Website createWebsite(String webName, String theme, Profile user, String alias, Session session) {
        Repository r = createRepository(webName, user, session);

        Website w = new Website();
        w.setOrganisation(this);
        w.setCreatedDate(new Date());
        w.setName(webName);
        w.setPublicTheme(theme);
        w.setInternalTheme(null);
        w.setCurrentBranch(Branch.TRUNK);
        w.setRepository(r);
        session.save(w);

        if (alias != null) {
            Website aliasWebsite = new Website();
            aliasWebsite.setOrganisation(this);
            aliasWebsite.setCreatedDate(new Date());
            aliasWebsite.setName(alias);
            aliasWebsite.setPublicTheme(null);
            aliasWebsite.setInternalTheme(null);
            aliasWebsite.setCurrentBranch(Branch.TRUNK);
            aliasWebsite.setRepository(r);
            aliasWebsite.setAliasTo(w);
            session.save(w);
        }

        return w;
    }

    public Organisation createChildOrg(String orgName, Session session) {
        Organisation o = new Organisation();
        o.setOrganisation(this);
        o.setName(orgName);
        o.setCreatedDate(new Date());
        o.setModifiedDate(new Date());
        session.save(o);
        return o;
    }        
    
    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public Group group(String groupName, Session session) {
        Group g = Group.findByOrgAndName(this, groupName, session);
        return g;
    }

    public List<Group> groups(Session session) {
        return Group.findByOrg(this, session);
    }

    public void delete(Session session) {
        if( getWebsites() != null ) {
            for( Website w : getWebsites()) {
                w.delete(session); 
            }
        }
        setWebsites(null);
        if( getMembers() != null ) {
            for( BaseEntity m : getMembers()) {
                m.delete(session);
            }
        }
        session.delete(this);
        
    }
}
