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

import io.milton.cloud.common.MutableCurrentDateService;
import io.milton.cloud.common.store.FileSystemBlobStore;
import io.milton.common.Path;
import io.milton.vfs.content.DbHashStore;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.HibernateException;
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
    HashStore hashStore;
    BlobStore blobStore;
    Organisation org;
    Profile user;
    MutableCurrentDateService currentDateService;
    SessionManager sessionManager;    
    
    @Before
    public void setUp() throws Exception {
        File db = new File("target/db.h2.db");
        if(db.exists()) {
            db.delete();
        }
        currentDateService = new MutableCurrentDateService();
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("database.xml");
        sessionFactory = (SessionFactory) appContext.getBean("sessionFactory");
        session = sessionFactory.openSession();
        
        File blobs = new File("target/blobs");
        if( blobs.exists()) {
            FileUtils.deleteDirectory(blobs);
        }
        
        hashStore = new DbHashStore();
        blobStore = new FileSystemBlobStore(blobs);
        
        org = new Organisation();
        org.setCreatedDate(currentDateService.getNow());
        org.setModifiedDate(currentDateService.getNow());
        org.setName("root");
        session.save(org);
        
        user = new Profile();
        user.setName("testUser");
        user.setAdminOrg(org);
        user.setCreatedDate(currentDateService.getNow());
        user.setModifiedDate(currentDateService.getNow());
        session.save(user);
        
        
        sessionManager = new SessionManager(sessionFactory);
        session = sessionManager.open();
                      
    }

    /**
     * Create a tree of resources, then copy the hash and make sure we have a copy
     * of the tree
     */
    @Test
    public void testCopy() {
        Branch b = initRepo();
        Transaction tx = session.beginTransaction();
        DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        DirectoryNode root = dataSession.getRootDataNode();
        DirectoryNode x1 = root.addDirectory("x1");
        x1.addFile("y1", 123);
        x1.addFile("y2", 123);
        x1.addFile("y3", 123);
        long newHash = dataSession.save(user);
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        root = dataSession.getRootDataNode();
        x1 = (DirectoryNode) dataSession.find(Path.path("/x1"));
        assertEquals(3, x1.size());
        root.addDirectory("x2", x1.getHash());
        newHash = dataSession.save(user);
        System.out.println("done copy, root hash is now: " + newHash);
        tx.commit();
        session.close();
        
        session = sessionFactory.openSession();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        DirectoryNode x2 = (DirectoryNode) dataSession.find(Path.path("/x2"));
        assertNotNull(x2);
        assertEquals(3, x2.size());
        session.close();        
        
        
    }
    
    
    @Test
    public void test() {
        Branch b = initRepo();
        
        Transaction tx = session.beginTransaction();
        DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        DirectoryNode root = dataSession.getRootDataNode();
        DirectoryNode x1 = root.addDirectory("x1");
        DirectoryNode x2 = root.addDirectory("x2");
        FileNode y = x1.addFile("y", 123);
        long newHash = dataSession.save(user);
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x1/y"));
        assertNotNull(y);
        x2 = (DirectoryNode) dataSession.find(Path.path("/x2"));
        assertNotNull(x2);
        y.move(x2, "y");
        newHash = dataSession.save(user);
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x2/y"));
        assertNotNull(y);
        y.delete();
        newHash = dataSession.save(user);
        System.out.println("creted new hash: " + newHash);
        tx.commit();        
        session.close();
        
        session = sessionFactory.openSession();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x2/y"));
        assertNull(y);
    }
    

    /**
     * Test of save method, of class ContentSession.
     */
    @Test
    public void test_BasicTransactionalManipulation() {
        Branch b = initRepo();
        
        Transaction tx = session.beginTransaction();
        DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        DirectoryNode root = dataSession.getRootDataNode();
        DirectoryNode x1 = root.addDirectory("x1");
        DirectoryNode x2 = root.addDirectory("x2");
        FileNode y = x1.addFile("y");
        y.setHash(111);
        dataSession.save(user);
        tx.commit();        
        session.close();
        
        session = sessionManager.open();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x1/y"));
        assertNotNull(y);
        x2 = (DirectoryNode) dataSession.find(Path.path("/x2"));
        assertNotNull(x2);
        y.move(x2, "y");
        dataSession.save(user);
        tx.commit();        
        session.close();        

        session = sessionManager.open();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x2/y"));
        assertNotNull(y);
        y.delete();
        dataSession.save(user);
        tx.commit();        
        session.close();        

        session = sessionManager.open();
        tx = session.beginTransaction();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x2/y2"));
        assertNull(y);
        session.close();                
    }

    
    @Test
    public void test_SettingGettingContent() throws IOException {
        Branch b = initRepo();
        
        Transaction tx = session.beginTransaction();
        DataSession dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        DirectoryNode root = dataSession.getRootDataNode();
        DirectoryNode x1 = root.addDirectory("x1");
        FileNode y = x1.addFile("y");
        byte[] buf = new byte[50000];
        Arrays.fill(buf, (byte)9);
        InputStream in = new ByteArrayInputStream(buf);
        y.setContent(in);
        dataSession.save(user);
        tx.commit();        
        session.close();
        
        // read it back
        session = sessionManager.open();
        dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
        y = (FileNode) dataSession.find(Path.path("/x1/y"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        y.writeContent(out);
        assertEquals(50000, out.size());
        session.close();           
    }

    private Branch initRepo() throws HibernateException {
        
        Organisation org = new Organisation();
        org.setName("test org");
        org.setCreatedDate(new Date());
        org.setModifiedDate(new Date());
        session.save(org);
        
        Repository r = new Repository();
        r.setBaseEntity(org);
        r.setCreatedDate(new Date());
        r.setName("test");
        r.setTitle("test repo");
        r.setBranches(new ArrayList<Branch>());
        session.save(r);
                
        Commit c = new Commit();
        c.setCreatedDate(new Date());
        c.setEditor(user);
        c.setItemHash(0);
        session.save(c);
        
        Branch b = new Branch();
        b.setRepository(r);
        b.setName("trunk");
        b.setCreatedDate(new Date());
        b.setHead(c);
        r.getBranches().add(b);
        session.save(b);
        return b;
    }      
}
