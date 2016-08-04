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
package com.gettingstarted.web;

import io.milton.annotations.ChildrenOf;
import io.milton.annotations.Delete;
import io.milton.annotations.Get;
import io.milton.annotations.MakeCollection;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.common.DefaultCurrentDateService;
import io.milton.cloud.common.store.FileSystemBlobStore;
import io.milton.common.ModelAndView;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.vfs.content.DbHashStore;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;
import org.hibernate.Transaction;


@ResourceController
public class VfsController {

    private boolean initDone;
    
    private HashStore hashStore = new DbHashStore();
    private BlobStore blobStore = new FileSystemBlobStore(new File("target/blobs"));
    private CurrentDateService currentDateService = new DefaultCurrentDateService();
    
    public VfsController() {
        File blobsDir = new File("target/blobs");
        System.out.println("Using blobs directory: " + blobsDir.getAbsolutePath());
        blobsDir.mkdirs();
        blobStore = new FileSystemBlobStore(blobsDir);
    }
            
    @Root
    public DataSession.DirectoryNode getRoot() {
        initDb();
        return getDataSession().getRootDataNode();
    }

    @ChildrenOf
    public List<DataSession.DataNode> getRootChildren(DataSession.DirectoryNode dir) {
        List<DataSession.DataNode> list = new ArrayList<DataSession.DataNode>();
        for( DataSession.DataNode n : dir) {
            list.add(n);
        }
        return list;
    }    
    
    @Get
    public ModelAndView renderDirectoryPage(DataSession.DirectoryNode dir) throws UnsupportedEncodingException {
        return new ModelAndView("directory", dir, "directory");
    }

    @PutChild
    public DataSession.FileNode uploadFile(DataSession.DirectoryNode dir, String newName, InputStream in) throws IOException {
        Transaction tx = SessionManager.beginTx();
        DataSession.FileNode file = dir.addFile(newName);
        file.setContent(in);
        getDataSession().save(getUser());
        SessionManager.commit(tx);
        return file;
    }
    
    @Get
    public void downloadFile(DataSession.FileNode file, OutputStream out) throws IOException {
        file.writeContent(out);
    }
    
    @Delete
    public void deleteNode(DataSession.DataNode node) throws IOException {
        Transaction tx = SessionManager.beginTx();
        node.delete();
        getDataSession().save(getUser());
        SessionManager.commit(tx);
    }
    
    @MakeCollection
    public DataSession.DirectoryNode makeDirectory(DataSession.DirectoryNode dir, String newName) throws IOException {
        Transaction tx = SessionManager.beginTx();
        DataSession.DirectoryNode newDir = dir.addDirectory(newName);
        getDataSession().save(getUser());
        SessionManager.commit(tx);            
        return newDir;
    }
    
        
    
    public Organisation getRootOrg() {
        return Organisation.findRoot(SessionManager.session());
    }
    
    public Repository getRepo() {
        return getRootOrg().repository("rootRepo");
    }
    
    public Branch getRootBranch() {
        return getRepo().liveBranch();
    }
    
    public DataSession getDataSession() {
        Request req = HttpManager.request();
        DataSession dataSession = (DataSession) req.getAttributes().get("DataSession");
        if( dataSession == null ) {
            Session session = SessionManager.session();
            dataSession = new DataSession(getRootBranch(), session, hashStore, blobStore, currentDateService);
            req.getAttributes().put("DataSession", dataSession);
        }
        return dataSession;
    }
    
    public DataSession.DirectoryNode getRootDir() {
        return getDataSession().getRootDataNode();
    }
    
    public Profile getUser() {
        return Profile.find("admin", SessionManager.session());
    }
    
    private synchronized void initDb() {
        if( initDone ) {
            return ;
        }        
        Organisation rootOrg;

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        rootOrg = Organisation.findRoot(session);
        if (rootOrg == null) {
            rootOrg = new Organisation();
            rootOrg.setOrgId("rootOrg");
            rootOrg.setModifiedDate(new Date());
            rootOrg.setCreatedDate(new Date());
            session.save(rootOrg);
            
            Profile user = new Profile();
            user.setName("admin");
            user.setCreatedDate(new Date());
            user.setModifiedDate(new Date());
            user.setEmail("admin@home.com");
            user.setNickName("admin");
            user.setEnabled(true);
            session.save(user);
            
            Group admins = rootOrg.createGroup("admins");
            admins.setRegistrationMode(Group.REGO_MODE_CLOSED);
            session.save(admins);
            
            user.addToGroup(admins, rootOrg, session);
            Repository repo = rootOrg.createRepository("rootRepo", user, session);
            System.out.println("created repo: " + repo.getName());
            Branch branch = repo.liveBranch();
            System.out.println("created branch: " + branch.getName());
        }
        tx.commit();    
        initDone = true;
    }
}
