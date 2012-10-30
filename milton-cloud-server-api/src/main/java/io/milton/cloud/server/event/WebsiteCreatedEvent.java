/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.event;

import io.milton.event.Event;
import io.milton.vfs.db.*;

/**
 * Fired when a user joins a group
 *
 * @author brad
 */
public class WebsiteCreatedEvent implements Event {


    private final Website website;

    public WebsiteCreatedEvent(Website website) {
        this.website = website;
    }

    
    
    public Website getWebsite() {
        return website;
    }

}
