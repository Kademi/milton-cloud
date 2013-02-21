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
package io.milton.cloud.server.db;

import io.milton.vfs.db.Group;
import io.milton.vfs.db.utils.DbUtils;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

/**
 * Represents an available opt-in for a group. Users are able to select this
 *
 * @author brad
 */
@javax.persistence.Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OptIn {

    /**
     * Find opt-ins which are attached to the given group
     * 
     * @param group
     * @return 
     */
    public static List<OptIn> findForGroup(Group group, Session session) {
        Criteria crit = session.createCriteria(OptIn.class);
        crit.add(Restrictions.eq("attachedToGroup", group));
        return DbUtils.toList(crit, OptIn.class);
    }
    
    public static OptIn create(Group attachedTo, Group optinGroup) {
        OptIn o = new OptIn();
        o.setAttachedToGroup(attachedTo);
        o.setOptinGroup(optinGroup);
        return o;
    }
    
    public static OptIn findOptin(List<OptIn> optins, Group g) {
        for (OptIn o : optins) {
            if (o.getOptinGroup() == g) {
                return o;
            }
        }
        return null;
    }    
    
    private long id;
    private Group attachedToGroup;
    private Group optinGroup;
    private String message;

    @Id
    @GeneratedValue          
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Group getAttachedToGroup() {
        return attachedToGroup;
    }

    public void setAttachedToGroup(Group attachedToGroup) {
        this.attachedToGroup = attachedToGroup;
    }

    @ManyToOne(optional=false)
    public Group getOptinGroup() {
        return optinGroup;
    }

    public void setOptinGroup(Group optinGroup) {
        this.optinGroup = optinGroup;
    }

    @Column(nullable=false, length=200)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
