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

import io.milton.vfs.db.NvSet;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
    
    public static List<CustomReport> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(CustomReport.class);
        crit.add(Restrictions.eq("organisation", org));
        return DbUtils.toList(crit, CustomReport.class);
    }    

    public static CustomReport create(Organisation org, String newName, String newTitle) {
        CustomReport r = new CustomReport();
        r.setName(newName);
        r.setTitle(newTitle);
        r.setOrganisation(org);
        return r;
    }
    
    private long id;
    private Organisation organisation;
    private String sourceClass;
    private String name;
    private String title;
    private NvSet fieldset; // optional, if present is a list of field names and their metadata for what to collect
    
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

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
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

    @ManyToOne
    public NvSet getFieldset() {
        return fieldset;
    }

    public void setFieldset(NvSet fieldset) {
        this.fieldset = fieldset;
    }

    

    
}
