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
package io.milton.cloud.server.web;

import io.milton.vfs.db.Profile;

/**
 *
 * @author brad
 */
public class ProfileBean {
    
    public static ProfileBean toBean(Profile p) {
        ProfileBean b = new ProfileBean();
        populateBean(p, b);
        return b;
    }

    public static void populateBean(Profile p, ProfileBean b) {
        b.setUserId(p.getId());
        b.setHref("/users/" + p.getName() + "/public" );
        b.setName(p.getNickName());
        if( b.getName() == null ) {
            b.setName(p.getName());
        }
        b.setPhotoHash(p.getPhotoHash());
    }
    
    
    private String href;
    private String name;
    private String photoHash;
    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(String photoHash) {
        this.photoHash = photoHash;
    }

    
    
    
}
