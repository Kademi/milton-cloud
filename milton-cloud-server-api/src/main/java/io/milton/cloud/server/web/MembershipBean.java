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
package io.milton.cloud.server.web;

import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.NvPair;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author brad
 */
public class MembershipBean {

    private Map<String, String> fields;
    private String groupName;
    private OrgData org;
    private transient GroupMembership gm; // transient to prevent serialisation
    private transient Long localCount;
    private transient ProfileBean profile;

    public MembershipBean(GroupMembership gm) {
        this.gm = gm;
        groupName = gm.getGroupEntity().getName();
        org = new OrgData(gm.getWithinOrg());
        fields = new HashMap<>();
        if (gm.getFields() != null && gm.getFields().getNvPairs() != null) {
            for (NvPair f : gm.getFields().getNvPairs()) {
                fields.put(f.getName(), f.getPropValue());
            }
        }
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getGroupName() {
        return groupName;
    }

    public OrgData getOrg() {
        return org;
    }

    /**
     * Find the number of members within this same group on this organisation
     *
     * @return
     */
    public long numLocalMembers() {
        if( localCount == null ) {
            localCount = GroupMembership.count(gm.getGroupEntity(), gm.getWithinOrg(), SessionManager.session());
        }
        return localCount;
    }
    
    public ProfileBean profile() {
        if( profile == null ) {
            profile = ProfileBean.toBean(gm.getMember());
        }
        return profile;
    }
}
