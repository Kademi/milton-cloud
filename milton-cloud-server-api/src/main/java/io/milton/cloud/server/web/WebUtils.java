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
package io.milton.cloud.server.web;

import io.milton.resource.Resource;


/**
 *
 * @author brad
 */
public class WebUtils {
    public static  RootFolder findRootFolder(Resource aThis) {
        if (aThis instanceof AbstractResource) {
            AbstractResource ar = (AbstractResource) aThis;
            return WebUtils.findRootFolder(ar);
        } else {
            return null;
        }
    }

    public static RootFolder findRootFolder(AbstractResource ar) {
        if (ar instanceof RootFolder) {
            return (RootFolder) ar;
        } else {
            return WebUtils.findRootFolder(ar.getParent());
        }
    }    
}
