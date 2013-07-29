/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.db;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

/**
 * List of fields in the CSV report are stored in fields, using syntax:
 * 
 * name=title|name2=title2|...
 *
 * @author brad
 */
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CustomReport implements Serializable{
    
    
    private long id;
    private Organisation organisation;
    private String name;
    private String title;
    private String fields;
    
    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    

    /**
     * User entered title of the reward
     * 
     * @return 
     */
    @Column(nullable=false)
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
        
   
    
    /**
     * The auto-assigned identifier for the reward. It should be derived from the
     * title, but is immutable from a user's perspective
     * 
     * @return 
     */
    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column
    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }


    
}
