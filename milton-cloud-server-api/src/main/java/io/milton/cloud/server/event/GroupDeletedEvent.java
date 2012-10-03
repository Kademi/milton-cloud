/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.event;

import io.milton.event.Event;
import io.milton.vfs.db.Group;

/**
 * Fired when a group is added or removed to a website
 *
 * @author brad
 */
public class GroupDeletedEvent implements Event{
    private final Group group;

    public GroupDeletedEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }        
}
