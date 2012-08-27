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
package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import javax.persistence.DiscriminatorValue;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;

/**
 * This process tracks a user's progress through a program;
 *
 * @author brad
 */
@javax.persistence.Entity
@DiscriminatorValue("P")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MembershipProcess extends BaseProcess {

    public static MembershipProcess find(GroupMembership membership, String name, Session session) {
        Criteria crit = session.createCriteria(MembershipProcess.class);
        crit.add(Expression.eq("moduleStatus", membership));
        crit.add(Expression.eq("processName", name));
        return DbUtils.unique(crit);
    }
    
    private GroupMembership membership;
    
    public GroupMembership getMembership() {
        return membership;
    }

    public void setMembership(GroupMembership membership) {
        this.membership = membership;
    }

    
}
