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

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;


/**
 *
 * @author brad
 */
public class WebUtils {
    public static  RootFolder findRootFolder(Resource aThis) {
        if (aThis instanceof CommonResource) {
            CommonResource ar = (CommonResource) aThis;
            return WebUtils.findRootFolder(ar);
        } else {
            throw new RuntimeException("Cant get root folder for: " + aThis.getClass());
        }
    }

    public static RootFolder findRootFolder(CommonResource ar) {
        if (ar instanceof RootFolder) {
            return (RootFolder) ar;
        } else {
            CommonCollectionResource parent = ar.getParent();
            if( parent == null ) {
                throw new RuntimeException("Got null parent from: " + ar.getClass() + " - " + ar.getName());
            }
            return WebUtils.findRootFolder(parent);
        }
    }    
    

    public static String findAutoName(CollectionResource folder, Map<String, String> parameters) {
        String nameToUse = getImpliedName(parameters, folder);
        if (nameToUse != null) {
            nameToUse = nameToUse.toLowerCase().replace("/", "");
            nameToUse = nameToUse.replace("'", "");
            nameToUse = nameToUse.replace("\"", "");
            nameToUse = nameToUse.replace("@", "-");
            nameToUse = nameToUse.replace(" ", "-");
            nameToUse = nameToUse.replace("?", "-");
            nameToUse = nameToUse.replace(":", "-");
            nameToUse = nameToUse.replace("--", "-");
            nameToUse = nameToUse.replace("--", "-");
            nameToUse = WebUtils.getUniqueName(folder, nameToUse);
        } else {
            nameToUse = WebUtils.getDateAsNameUnique(folder);
        }
        return nameToUse;
    }

    private static String getImpliedName(Map<String, String> parameters, CollectionResource folder) {
        if (parameters == null) {
            return null;
        }
        if (parameters.containsKey("name")) {
            String name = parameters.get("name");
            return name;
        } else if (parameters.containsKey("_counter")) {
            String name = parameters.get("_counter");
            return name;
        } else if (parameters.containsKey("fullName")) {
            return parameters.get("fullName");
        } else if (parameters.containsKey("firstName")) {
            String fullName = parameters.get("firstName");
            if (parameters.containsKey("surName")) {
                fullName = fullName + "." + parameters.get("surName");
            }
            return fullName;
        } else if (parameters.containsKey("title")) {
            String title = parameters.get("title");
            return title;
        } else {
            return null;
        }
    }    
    

    public static String getDateAsName() {
        return getDateAsName(false);
    }

    public static String getDateAsName(boolean seconds) {
        Date dt = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        String name = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.MONTH) + "_" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE);
        if (seconds) {
            name += "_" + cal.get(Calendar.SECOND);
        }
        return name;
    }

    public static String getDateAsNameUnique(CollectionResource col) {
        String name = getDateAsName();
        return getUniqueName(col, name);
    }

    public static String getUniqueName(CollectionResource col, final String baseName) {
        try {
            String name = baseName;
            Resource r = col.child(name);
            int cnt = 0;            
            while (r != null) {
                cnt++;
                name = baseName + cnt;
                r = col.child(name);
            }
            return name;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String pad(int i) {
        if (i < 10) {
            return "000" + i;
        } else if (i < 100) {
            return "00" + i;
        } else if (i < 1000) {
            return "0" + i;
        } else {
            return i + "";
        }
    }    
}
