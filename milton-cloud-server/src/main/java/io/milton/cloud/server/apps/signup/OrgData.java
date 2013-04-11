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
package io.milton.cloud.server.apps.signup;

import io.milton.vfs.db.Organisation;

/**
 *
 * @author brad
 */
public class OrgData {
    private String orgId;
    private String title;
    private String address;
    private String addressLine2;
    private String addressState;
    private String phone;

    public OrgData(Organisation org) {
        orgId = org.getOrgId();
        title = org.getTitle();
        address = org.getAddress();
        addressLine2 = org.getAddressLine2();
        addressState = org.getAddressState();
        phone = org.getPhone();
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
    
}
