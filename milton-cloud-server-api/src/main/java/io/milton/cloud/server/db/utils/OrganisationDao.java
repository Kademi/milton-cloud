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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import io.milton.vfs.db.Organisation;

/**
 *
 * @author brad
 */
public class OrganisationDao {

    public static Organisation getOrganisation(String name, Session session) {
        return (Organisation) session.get(Organisation.class, name);
    }

    public static Organisation getRootOrg(Session session) {
        Criteria crit = session.createCriteria(Organisation.class);
        crit.add(Expression.isNull("organisation"));
        Organisation org = (Organisation) crit.uniqueResult();
        return org;
    }

    public static Organisation getOrganisation(Organisation organisation, String name, Session session) {        
        Criteria crit = session.createCriteria(Organisation.class);
        crit.add(
                Expression.and(
                    Expression.eq("organisation", organisation), 
                    Expression.eq("name", name)
                )
        );        
        Organisation org = (Organisation) crit.uniqueResult();
        return org;
    }
}
