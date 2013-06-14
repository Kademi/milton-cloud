/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import static io.milton.cloud.server.web.ProfileBean.populateBean;
import io.milton.vfs.db.Profile;

/**
 * Just like a normal profile bean, but with extra stuff that admins can see
 *
 * @author brad
 */
public class ExtProfileBean extends ProfileBean{
    
    public static ExtProfileBean toBeanExt(Profile p) {
        ExtProfileBean b = new ExtProfileBean();
        ProfileBean.populateBean(p, b);
        b.setEmail(p.getEmail());
        b.setSurName(p.getSurName());
        b.setFirstName(p.getFirstName());
        return b;
    }        
    
    private String email; 
    private String firstName;
    private String surName;  

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }
    
    
}
