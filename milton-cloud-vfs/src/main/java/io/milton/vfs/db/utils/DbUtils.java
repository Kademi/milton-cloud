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
package io.milton.vfs.db.utils;

import io.milton.common.FileUtils;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DbUtils {

    private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

    /**
     * Just casts the list, and ensures it never returns null.
     *
     * @param <T>
     * @param crit
     * @param c
     * @return
     */
    public static <T> List<T> toList(Criteria crit, Class<T> c) {
        List list = crit.list();
        if (list == null) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }

    public static <T> T unique(Criteria crit) {
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            if (log.isInfoEnabled()) {
                log.info("Multiple items returned from query=" + list.size());
                for (Object o : list) {
                    Serializable id = SessionManager.session().getIdentifier(o);
                    log.info(" - class=" + o.getClass() + " id=" + id);
                }
            }
        }
        T item = (T) list.get(0);
        return item;
    }

    public static long asLong(List results, int columnIndex) {
        if (results == null) {
            return 0;
        }
        Object o = results.get(columnIndex);
        return asLong(o);
    }
    
    public static long asLong(Object o) {
        if (o instanceof Long) {
            Long num = (Long) o;
            return num;
        } else if (o instanceof Integer) {
            Integer ii = (Integer) o;
            return ii;
        } else {
            if (o != null) {
                log.error("Unsupported value type: " + o.getClass());
            }
            return 0;
        }        
    }

    public static String incrementFileName(String name, boolean isFirst) {
        String mainName = FileUtils.stripExtension(name);
        String ext = FileUtils.getExtension(name);
        int count;
        if (isFirst) {
            count = 1;
        } else {
            int pos = mainName.lastIndexOf("-");
            if (pos > 0) {
                String sNum = mainName.substring(pos + 1, mainName.length());
                count = Integer.parseInt(sNum) + 1;
                mainName = mainName.substring(0, pos);
            } else {
                count = 1;
            }
        }
        String s = mainName + "-" + count;
        if (ext != null) {
            s = s + "." + ext;
        }
        return s;

    }

    /**
     * Change any characters that might be ugly in a url with hyphens.
     *
     * @param baseName
     * @return
     */
    public static String replaceYuckyChars(String baseName) {
        String nameToUse = baseName;
        nameToUse = nameToUse.toLowerCase().replace("/", "");
        nameToUse = nameToUse.replaceAll("[^A-Za-z0-9]", "-");
        while (nameToUse.contains("--")) {
            nameToUse = nameToUse.replace("--", "-");
        }
        if (nameToUse.endsWith("-")) {
            nameToUse = nameToUse.substring(0, nameToUse.length() - 1);
        }
        return nameToUse;
    }
}
