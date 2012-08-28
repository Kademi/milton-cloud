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
package io.milton.cloud.server.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;

/**
 * A named counter provides a means of generating monotonic-ally increasing
 * series.
 *
 *
 *
 * @author brad
 */
@Entity
public class NamedCounter implements Serializable {

    public static NamedCounter findByName(String name, Session session) {
        Criteria crit = session.createCriteria(NamedCounter.class);
        crit.add(Restrictions.eq("name", name));
        NamedCounter c = DbUtils.unique(crit);
        return c;
    }        
    
    public static NamedCounter findOrCreate(String name, Session session) {
        NamedCounter c = findByName(name, session);
        if( c == null ) {
            System.out.println("counter not found: " + name);
            c = new NamedCounter();
            c.setName(name);
            c.setCounter(1);
            session.save(c);
        } else {
            System.out.println("found counter: " + name + " - " + c.getCounter());
        }
        return c;
    }
    
    /**
     * Uses syncronised access for locking. Not very good but given low concurrency
     * expectations for the target use case (ie content creation) it should be 
     * good enough for the time being
     * 
     * @param name
     * @param session
     * @return 
     */
    public synchronized static long increment(String name, Session session) {
        NamedCounter c = findOrCreate(name, session);
        c.setCounter(c.getCounter()+1);
        session.save(c);
        System.out.println("saved counter: " + c.getCounter());
        return c.getCounter();
    }
    
    private long id;
    private String name;
    private long counter;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }
}
