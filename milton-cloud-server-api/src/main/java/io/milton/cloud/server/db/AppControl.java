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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

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
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "organisation", "website_branch"})}// item names must be unique within a directory
)
public class AppControl implements Serializable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AppControl.class);

    public static List<AppControl> find(Branch websiteBranch, Session session) {
        final Criteria crit = session.createCriteria(AppControl.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("websiteBranch", websiteBranch));
        return DbUtils.toList(crit, AppControl.class);
    }

    public static List<AppControl> find(Organisation c, Session session) {
        final Criteria crit = session.createCriteria(AppControl.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("organisation", c));
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

    public static AppControl find(Branch websiteBranch, String appId, Session session) {
        List<AppControl> list = find(websiteBranch, session);
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
        List<AppControl> list = find(organisation, session);
        if (list != null) {
            for (AppControl ac : list) {
                if (ac.getName().equals(appId)) {
                    session.delete(ac);
                }
            }
        }

        AppControl ac = new AppControl();
        ac.setName(appId);
        ac.setOrganisation(organisation);

        ac.setEnabled(enabled);
        ac.setModifiedBy(currentUser);
        ac.setModifiedDate(currentDate);
        session.save(ac);
        return ac;
    }

    public static AppControl setStatus(String appId, Branch websiteBranch, boolean enabled, Profile currentUser, Date currentDate, Session session) {
        List<AppControl> list = find(websiteBranch, session);
        if (list != null) {
            for (AppControl ac : list) {
                if (ac.getName().equals(appId)) {
                    session.delete(ac);
                }
            }
        }

        AppControl ac = new AppControl();
        ac.setName(appId);
        ac.setWebsiteBranch(websiteBranch);
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
    public static void initApps(List<String> appIds, Organisation organisation, Profile currentUser, Date now, Session session) {
        for (String appId : appIds) {
            AppControl newac = new AppControl();
            newac.setEnabled(true);
            newac.setName(appId);
            newac.setOrganisation(organisation);
            newac.setModifiedBy(currentUser);
            newac.setModifiedDate(now);
            session.save(newac);
        }
    }

    public static void initDefaultApps(Branch websiteBranch, Profile currentUser, Date now, Session session) {
        Organisation org = (Organisation) websiteBranch.getRepository().getBaseEntity();
        for (AppControl ac : find(org, session)) {
            AppControl newac = new AppControl();
            if (ac.isEnabled()) {
                newac.setEnabled(true);
                newac.setName(ac.getName());
                newac.setWebsiteBranch(websiteBranch);
                newac.setModifiedBy(currentUser);
                newac.setModifiedDate(now);
                session.save(newac);
            }

        }
    }
    private long id;
    private Branch branch; // the branch of the website the app control is affecting
    private Organisation organisation;
    private String name; // the name of the application
    private boolean enabled; // if the app is enabled for the specified container
    private Profile modifiedBy;
    private Date modifiedDate;
    private List<AppSetting> appSettings;

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
    public Branch getWebsiteBranch() {
        return branch;
    }

    public void setWebsiteBranch(Branch branch) {
        this.branch = branch;
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

    @ManyToOne(optional = false)
    public Profile getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Profile modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @OneToMany(mappedBy = "appControl")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<AppSetting> getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(List<AppSetting> appSettings) {
        this.appSettings = appSettings;
    }

    public String getSetting(String setting) {
        if (getAppSettings() != null) {
            for (AppSetting appSetting : getAppSettings()) {
                if (appSetting.getName().equals(setting)) {
                    return appSetting.getPropValue();
                }
            }
        }
        return null;
    }

    public void setSetting(String name, String settingValue, Session session) {
        AppSetting setting = null;
        if (getAppSettings() != null) {
            for (AppSetting appSetting : getAppSettings()) {
                if (appSetting.getName().equals(name)) {
                    setting = appSetting;
                    break;
                }
            }
        }
        if (setting == null) {
            if (settingValue == null) {
                return; // DONE
            }
            setting = new AppSetting();
            setting.setAppControl(this);
            setting.setName(name);
            if (getAppSettings() == null) {
                setAppSettings(new ArrayList<AppSetting>());
            }
            getAppSettings().add(setting);
        }
        if (settingValue == null) {
            session.delete(setting);
        } else {
            setting.setPropValue(settingValue);
            session.save(setting);
        }
    }

    /**
     * Copy this object and its parameters to the new branch
     *
     * @param newBranch
     * @param session
     */
    public AppControl copyTo(Branch newBranch, Profile currentUser, Date now, Session session) {
        AppControl newac = new AppControl();
        newac.setEnabled(isEnabled());
        newac.setModifiedBy(currentUser);
        newac.setModifiedDate(now);
        newac.setName(name);
        newac.setOrganisation(organisation);
        newac.setWebsiteBranch(newBranch);
        session.save(newac);

        if (getAppSettings() != null) {
            for (AppSetting setting : getAppSettings()) {
                newac.setSetting(setting.getName(), setting.getPropValue(), session);
            }
        }
        return newac;
    }
}
