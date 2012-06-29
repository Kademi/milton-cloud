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
package io.milton.cloud.server.apps;

import io.milton.resource.AccessControlledResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.util.List;

/**
 * Application extension which permits applicationst to append priviledges
 * to a resource's ACL
 *
 * @author brad
 */
public interface PriviledgeApplication extends Application{
    void appendPriviledges(List<AccessControlledResource.Priviledge> list, Profile user, Resource r);
}
