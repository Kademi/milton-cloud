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
package io.milton.vfs.data;

import io.milton.common.Path;
import io.milton.vfs.data.DataSession.DataNode;
import java.io.File;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class DataSessionTest {
    
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
    
    @Test
    public void test() {
        Transaction tx = session.beginTransaction();
        DataSession dataSession = new DataSession(0, session);
        DataNode root = dataSession.getRootDataNode();
        DataNode x1 = root.add("x1", 1, "d");
        DataNode x2 = root.add("x2", 1, "d");
        DataNode y = x1.add("y", 123, "f");
        long newHash = dataSession.save();
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        dataSession = new DataSession(newHash, session);
        y = dataSession.find(Path.path("/x1/y"));
        assertNotNull(y);
        x2 = dataSession.find(Path.path("/x2"));
        assertNotNull(x2);
        y.move(x2, "y");
        newHash = dataSession.save();
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        dataSession = new DataSession(newHash, session);
        y = dataSession.find(Path.path("/x2/y"));
        assertNotNull(y);
        y.delete();
        newHash = dataSession.save();
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        dataSession = new DataSession(newHash, session);
        y = dataSession.find(Path.path("/x2/y"));
        assertNull(y);
    }
}
