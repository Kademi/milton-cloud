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
import org.hibernate.criterion.Expression;

/**
 * A Website is an alias for a repository. The name of the website is the DNS name
 *
 * @author brad
 */
@Entity
public class Website implements Serializable {
    public static List<Website>  findByWebsite(Repository repository, Session session) {
        Criteria crit = session.createCriteria(Website.class);
        crit.add(Expression.eq("repository", repository));
        return DbUtils.toList(crit, Website.class);
    }    
    
    private Organisation organisation;
    private long id;
    private String name; // identifies the resource to webdav
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
    
    @Column(length = 255, nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if( repository.getBranches() == null ) {
            return null;
        }
        for( Branch b : repository.getBranches() ) {
            if( b.getName().equals(getCurrentBranch())) {
                return b;
            }
        }
        return null;
    }

    @ManyToOne(optional=false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @ManyToOne(optional=false)
    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    
}
