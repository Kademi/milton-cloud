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
package io.milton.sync;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author brad
 */
public class SyncJob implements Serializable{
    private File localDir;
    private String remoteAddress;
    private String user;
    private String pwd;
    private boolean monitor;
    private boolean localReadonly;

    public SyncJob() {
    }
    
    
    public SyncJob(File localDir, String sRemoteAddress, String user, String pwd, boolean monitor, boolean readonlyLocal) {
        this.localDir = localDir;
        this.remoteAddress = sRemoteAddress;
        this.user = user;
        this.pwd = pwd;
        this.monitor = monitor;
        this.localReadonly = readonlyLocal;
        try {
            if (!localDir.exists()) {
                throw new RuntimeException("Sync dir does not exist: " + localDir.getCanonicalPath());
            } else if (!localDir.isDirectory()) {
                throw new RuntimeException("Sync path is not a directory: " + localDir.getCanonicalPath());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
    
    

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    

    public File getLocalDir() {
        return localDir;
    }

    public void setLocalDir(File localDir) {
        this.localDir = localDir;
    }
    
    

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
    
    

    public boolean isMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }
    
    

    public boolean isLocalReadonly() {
        return localReadonly;
    }

    public void setLocalReadonly(boolean localReadonly) {
        this.localReadonly = localReadonly;
    }
    
    
    
}
