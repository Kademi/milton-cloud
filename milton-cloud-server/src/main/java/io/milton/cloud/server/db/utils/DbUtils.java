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
package io.milton.cloud.server.db.utils;

import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;

/**
 *
 * @author brad
 */
public class DbUtils {
    /**
     * Just casts the list, and ensures it never returns null.
     * @param <T>
     * @param crit
     * @param c
     * @return 
     */
    public static <T> List<T> toList(Criteria crit, Class<T> c) {
        List list = crit.list();
        if( list == null ) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }
}
