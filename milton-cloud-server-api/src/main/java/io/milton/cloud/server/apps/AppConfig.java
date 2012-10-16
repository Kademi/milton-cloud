package io.milton.cloud.server.apps;

import io.milton.cloud.server.db.AppControl;
import io.milton.context.RootContext;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Properties;

/**
 *
 * @author brad
 */
public class AppConfig {

    private final String applicationId;
    private final Properties properties;
    private final RootContext context;

    public AppConfig(String applicationId, Properties properties, RootContext context) {
        this.applicationId = applicationId;
        this.properties = properties;
        this.context = context;
    }

    public RootContext getContext() {
        return context;
    }

    public Integer getInt(String name) {
        String s = properties.getProperty(name);
        if (s == null) {
            return null;
        } else {
            return Integer.parseInt(s);
        }
    }

    public void setInt(String name, Integer value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value.toString());
        }
    }

    public void add(String name, String val) {
        properties.put(name, val);
    }

    /**
     * Get a statically configured system wide property for this application
     *
     * @param key
     * @return
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a setting for this application within the RootFolder given (ie an
     * organisation or website setting)
     *
     * @param setting
     * @param rootFolder
     * @return
     */
    public String get(String setting, Organisation org) {
        AppControl appControl = AppControl.find(org, applicationId, SessionManager.session());        
        if( appControl == null ) {
            return null;
        }
        return appControl.getSetting(setting);
    }
    
    public String get(String setting, Branch b) {
        AppControl appControl = AppControl.find(b, applicationId, SessionManager.session());
        if( appControl == null ) {
            return null;
        }
        return appControl.getSetting(setting);
    }    
    
    public void set(String settingName, Organisation org, String settingValue) {
        AppControl appControl = AppControl.find(org, applicationId, SessionManager.session());
        if( appControl == null ) {
            throw new RuntimeException("Cant save setting because there is no Appcontrol record");
        }
        appControl.setSetting(settingName, settingValue, SessionManager.session());
        
    }
    
    public void set(String settingName, Branch websiteBranch, String settingValue) {
        AppControl appControl = AppControl.find(websiteBranch, applicationId, SessionManager.session());
        if( appControl == null ) {
            throw new RuntimeException("Cant save setting because there is no Appcontrol record");
        }
        appControl.setSetting(settingName, settingValue, SessionManager.session());
        
    }    
}
