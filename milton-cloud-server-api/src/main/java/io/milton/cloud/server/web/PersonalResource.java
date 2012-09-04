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
package io.milton.cloud.server.web;

import io.milton.vfs.db.Profile;

/**
 * Used to mark resources which can belong to a user. The security manager
 * uses this information to allow users to their own resources, regardless of
 * other permission systems.
 *
 * @author brad
 */
public interface PersonalResource {
    /**
     * Get the user who owns this resource
     * 
     * @return 
     */
    Profile getOwnerProfile();
}
