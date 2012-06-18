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
package io.milton.vfs.meta;

import io.milton.common.Path;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.MetaItem;
import io.milton.vfs.meta.MetaSession.MetaNode;
import java.io.File;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author brad
 */
public class MetaSessionTest {
    
    SessionFactory sessionFactory;
    Session session;
    
    @Before
    public void setUp() throws Exception {
        File db = new File("target/db.h2.db");
        if(db.exists()) {
            db.delete();
        }
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("database.xml");
        sessionFactory = (SessionFactory) appContext.getBean("sessionFactory");
        session = sessionFactory.openSession();
    }

    /**
     * Test of get method, of class MetaSession.
     */
    @Test
    public void test() {
        Transaction tx = session.beginTransaction();
        
        MetaItem rootMetaItem = new MetaItem();
        rootMetaItem.setCreatedDate(new Date());
        rootMetaItem.setModifiedDate(new Date());
        session.save(rootMetaItem);
        
        MetaSession metaSession = new MetaSession(rootMetaItem, session);
        MetaNode root = metaSession.getRootNode();
        MetaNode x1 = root.add("x1", session);
        MetaNode x2 = root.add("x2", session);
        MetaNode y = x1.add("y", session);
        tx.commit();        
        session.close();        
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        metaSession = new MetaSession(rootMetaItem, session);
        y = metaSession.get(Path.path("/x1/y"));
        x2 = metaSession.get(Path.path("/x2"));
        assertNotNull(y);
        assertNotNull(x2);
        y.move(x2, "y");
        tx.commit();        
        session.close();        
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        metaSession = new MetaSession(rootMetaItem, session);
        y = metaSession.get(Path.path("/x2/y"));
        assertNotNull(y);
        y.delete();
        tx.commit();        
        session.close();        

        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        metaSession = new MetaSession(rootMetaItem, session);
        y = metaSession.get(Path.path("/x2/y"));
        assertNull(y);
        session.close();        
        
    }
}
