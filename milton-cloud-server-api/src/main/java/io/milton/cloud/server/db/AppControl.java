/*
 * Copyright 2012 McEvoy Software Ltd.
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

import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 * Controls whether applications are enabled or not for an application container
 *
 * An application container can be: - a website - an organisation
 *
 * Applications can only be enabled if they are available. They are only
 * available if they are enabled on a parent container, and root containers have
 * all apps available.
 *
 * @author brad
 */
@javax.persistence.Entity
public class AppControl implements Serializable {

    public static List<AppControl> find(Website c, Session session) {
        final Criteria crit = session.createCriteria(AppControl.class);
        crit.add(Expression.eq("website", c));
        return DbUtils.toList(crit, AppControl.class);
    }

    public static List<AppControl> find(Organisation c, Session session) {
        final Criteria crit = session.createCriteria(AppControl.class);
        crit.add(Expression.eq("organisation", c));
        return DbUtils.toList(crit, AppControl.class);
    }

    public static AppControl find(Organisation c, String appId, Session session) {
        List<AppControl> list = find(c, session);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (AppControl ac : list) {
            if (ac.getName().equals(appId)) {
                return ac;
            }
        }
        return null;
    }
    
    public static AppControl find(Website c, String appId, Session session) {
        List<AppControl> list = find(c, session);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (AppControl ac : list) {
            if (ac.getName().equals(appId)) {
                return ac;
            }
        }
        return null;
    }
    

    public static AppControl setStatus(String appId, Organisation organisation, boolean enabled, Profile currentUser, Date currentDate, Session session) {
        AppControl ac = find(organisation, appId, session);
        if( ac == null ) {
            ac = new AppControl();
            ac.setName(appId);
            ac.setOrganisation(organisation);            
        }
        ac.setEnabled(enabled);
        ac.setModifiedBy(currentUser);
        ac.setModifiedDate(currentDate);
        session.save(ac);
        return ac;
    }

    public static AppControl setStatus(String appId, Website website, boolean enabled, Profile currentUser, Date currentDate, Session session) {
        AppControl ac = find(website, appId, session);
        if( ac == null ) {
            ac = new AppControl();
            ac.setName(appId);
            ac.setWebsite(website);            
        }
        ac.setEnabled(enabled);
        ac.setModifiedBy(currentUser);
        ac.setModifiedDate(currentDate);
        session.save(ac);
        return ac;
        
    }

    /**
     * Enable all available apps for the given organisation
     * 
     * @param organisation
     * @param session 
     */
    public static void initDefaultApps(Organisation organisation, Profile currentUser, Date now, Session session) {
        if( organisation.getOrganisation() == null ) {
            // is root org, so nothing to do
        }
        for( AppControl ac : find(organisation.getOrganisation(), session)) {
            AppControl newac = new AppControl();
            if( ac.isEnabled()) {
                newac.setEnabled(true);
                newac.setName(ac.getName());
                newac.setOrganisation(organisation);
                newac.setModifiedBy(currentUser);
                newac.setModifiedDate(now);
                session.save(newac);
            }
            
        }
    }
    
    public static void initDefaultApps(Website website, Profile currentUser, Date now, Session session) {

        for( AppControl ac : find(website, session)) {
            AppControl newac = new AppControl();
            if( ac.isEnabled()) {
                newac.setEnabled(true);
                newac.setName(ac.getName());
                newac.setWebsite(website);
                newac.setModifiedBy(currentUser);
                newac.setModifiedDate(now);
                session.save(newac);
            }
            
        }
    }    
    
    
    private long id;
    private Website website; // the container for the application will either be a website or an organisation
    private Organisation organisation;
    private String name; // the name of the application
    private boolean enabled; // if the app is enabled for the specified container
    private Profile modifiedBy;
    private Date modifiedDate;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    @ManyToOne
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @ManyToOne(optional=false)
    public Profile getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Profile modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)        
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    
}
