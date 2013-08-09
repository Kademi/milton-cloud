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
package io.milton.cloud.server.apps;

import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.UserResource;
import io.milton.resource.Resource;
import java.util.Map;
import org.apache.velocity.context.Context;

/**
 * An application which inserts objects into the templating context just prior
 * to rendering pages
 *
 * @author brad
 */
public interface TemplatingApplication {
    /**
     * The application should insert any objects here to be used by templating 
     * code. Note that this will be called for every templated page, even if these objects are not used, so you should
     * make sure that this is a cheap action and anything expensive (eg loading data)
     * only occurs if it is actually used
     * 
     * @param datamodel - insert objects in here
     * @param rootFolder - the folder the current page is within
     * @param page - the current page being rendered
     * @param params - request params
     * @param user  - the current user, or null
     */
    void appendTemplatingObjects(Context datamodel, RootFolder rootFolder, Resource page, Map<String, String> params, UserResource user);
}
