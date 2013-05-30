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

import io.milton.vfs.db.Organisation;

/**
 *
 * @author brad
 */
public class OrgData {
    private long id;
    private String orgId;
    private String title;
    private String address;
    private String addressLine2;
    private String addressState;
    private String phone;
    private String state;
    private String postcode;

    public OrgData(Organisation org) {
        id = org.getId();
        orgId = org.getOrgId();
        title = org.getTitle();
        address = org.getAddress();
        addressLine2 = org.getAddressLine2();
        addressState = org.getAddressState();
        phone = org.getPhone();
        state = org.getAddressState();
        postcode = org.getPostcode();
    }

    public long getId() {
        return id;
    }   
    
    public String getOrgId() {
        return orgId;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressState() {
        return addressState;
    }

    public String getPhone() {
        return phone;
    }

    public String getState() {
        return state;
    }

    public String getPostcode() {
        return postcode;
    }            
}
