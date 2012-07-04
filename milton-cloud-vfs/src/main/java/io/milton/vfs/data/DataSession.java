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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.common.ITriplet;
import io.milton.common.Path;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.DataItem;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.hashsplit4j.api.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 * A DataSession provides a simple way to read and write the versioned content
 * repository
 *
 * Simply locate nodes, move them and modify them as you would expect through
 * the API, then when the session is saved the complete state of the repository
 * is updated
 *
 * @author brad
 */
public class DataSession {

    private DirectoryNode rootDataNode;
    private final Session session;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final HashCalc hashCalc = HashCalc.getInstance();
    private final CurrentDateService currentDateService;
    private final Branch branch;

    public DataSession(Branch branch, Session session, HashStore hashStore, BlobStore blobStore, CurrentDateService currentDateService) {
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.session = session;
        this.branch = branch;
        this.currentDateService = currentDateService;
        Commit c = branch.latestVersion(session);
        long hash = 0;
        if (c != null) {
            hash = c.getItemHash();
        }
        rootDataNode = new DirectoryNode(null, null, hash);
    }

    public DirectoryNode getRootDataNode() {
        return rootDataNode;
    }

    public DataNode find(Path path) {
        if (path.isRoot()) {
            return rootDataNode;
        } else {
            DataNode parent = find(path.getParent());
            if (parent == null) {
                return null;
            } else if (parent instanceof DirectoryNode) {
                DirectoryNode dirNode = (DirectoryNode) parent;
                return dirNode.get(path.getName());
            } else {
                return null;
            }
        }
    }

    public List<DataItem> find(long hash) {
        Criteria crit = session.createCriteria(DataItem.class);
        crit.add(Expression.eq("parentHash", hash));
        return DbUtils.toList(crit, DataItem.class);
    }

    /**
     * Saves any changed items in the session by recalculating hashes and
     * persisting new items to the repository, and returns a list of root tree
     * items which represent changed trees. That list will contain the root of
     * the tree which this session directly represents, but it will also contain
     * the roots of trees which have been affected through linked item updates
     *
     * @return
     */
    public long save(Profile currentUser) {
        recalcHashes(rootDataNode);

        // persist stuff
        saveDir(rootDataNode);
        long newHash = rootDataNode.hash;

        Commit newCommit = new Commit();
        newCommit.setCreatedDate(currentDateService.getNow());
        newCommit.setEditor(currentUser);
        newCommit.setItemHash(newHash);
        session.save(newCommit);
        branch.setHead(newCommit);
        session.save(branch);

        return rootDataNode.hash;
    }

    private void recalcHashes(DataNode item) {
        if (item.dirty == null) {
            return; // not dirty, which means no children are dirty
        }
        // only directories have derived hashes
        if (item instanceof DirectoryNode) {
            DirectoryNode dirNode = (DirectoryNode) item;
            for (DataNode child : dirNode) {
                recalcHashes(child);
            }
            long newHash = hashCalc.calcHash(dirNode);
            item.setHash(newHash);
        }
    }

    private void saveDir(DirectoryNode item) {
        if (item.dirty == null) {
            return; // not dirty, which means no children are dirty
        }
        // the directory list to save might already be saved, in which case
        // unique constraints will prevent the insertion, so check first
        List<DataItem> existing = find(item.hash);
        if (existing != null && !existing.isEmpty()) {
            return; // already in db
        }
        for (DataNode child : item) {
            insertMember(item.hash, child);
        }
    }

    private void insertMember(long parentHash, DataNode item) {
        DataItem newItem = new DataItem();
        newItem.setItemHash(item.getHash());
        newItem.setName(item.getName());
        newItem.setType(item.getType());
        newItem.setParentHash(parentHash);
        session.save(newItem);
        if (item instanceof DirectoryNode) {
            saveDir((DirectoryNode) item);
        }
    }

    /**
     * Represents a logical view of a member in the versioned content
     * repository.
     *
     *
     * An item implements Iterable so its children can be iterated over, and
     * mutating operations must be performed through the DataNode methods.
     */
    public abstract class DataNode implements ITriplet {

        protected DirectoryNode parent;
        protected String name;
        protected String type;
        protected long hash;
        protected long loadedHash; // holds the hash value from when the node was loaded
        protected Boolean dirty;

        /**
         * Copy just creates the same type of item with the same hash
         * 
         * @param newDir
         * @param newName 
         */
        public abstract void copy(DirectoryNode newDir, String newName);
                
        private DataNode(DirectoryNode parent, String name, String type, long hash) {
            this.parent = parent;
            this.name = name;
            this.type = type;
            this.hash = hash;
            this.loadedHash = hash; 
        }

        /**
         * Move this item to a new parent, or to a new name, or both
         *
         * @param newParent
         */
        public void move(DirectoryNode newParent, String name) {
            DirectoryNode oldParent = this.getParent();
            if (oldParent != newParent) {
                this.setParent(newParent);
                if (oldParent.members != null) {
                    oldParent.members.remove(this);
                }
                newParent.getChildren().add(this);
            }
            if (name.equals(getName())) {
                setName(name);
            }
            parent.checkConsistency(this);
        }

        public void delete() {
            parent.getChildren().remove(this);
            parent.checkConsistency(parent);
            setDirty();
        }

        public DirectoryNode getParent() {
            return parent;
        }

        private void setParent(DirectoryNode parent) {
            this.parent = parent;
        }

        @Override
        public String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
            setDirty();
        }

        @Override
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public long getHash() {
            return hash;
        }

        public void setHash(long hash) {
            this.hash = hash;
            setDirty();
        }

        public long getLoadedHash() {
            return loadedHash;
        }
        
        
        protected void setDirty() {
            if (dirty != null) {
                return; // already set
            }
            dirty = Boolean.TRUE;
            if (parent != null) {
                parent.setDirty();
            }
        }

    }

    public class DirectoryNode extends DataNode implements Iterable<DataNode> {

        private List<DataNode> members;

        public DirectoryNode(DirectoryNode parent, String name, long hash) {
            super(parent, name, "d", hash);
        }

        @Override
        public void copy(DirectoryNode newDir, String newName) {
            newDir.addDirectory(newName, this.hash);
        }
                
        private List<DataNode> getChildren() {
            if (members == null) {
                List<DataItem> list = find(hash);
                members = new ArrayList<>();
                for (DataItem i : list) {
                    DataNode c;
                    if (i.getType().equals("d")) {
                        c = new DirectoryNode(this, i.getName(), i.getItemHash());
                    } else {
                        c = new FileNode(this, i.getName(), i.getItemHash());
                    }
                    members.add(c);
                }
            }
            return members;
        }

        public FileNode addFile(String name) {
            FileNode item = new FileNode(this, name, 0);
            getChildren().add(item);
            setDirty();
            return item;
        }

        public FileNode addFile(String name, long hash) {
            FileNode item = new FileNode(this, name, hash);
            getChildren().add(item);
            setDirty();
            return item;
        }

        public DirectoryNode addDirectory(String name, long hash) {
            DirectoryNode item = new DirectoryNode(this, name, hash);
            getChildren().add(item);
            setDirty();
            return item;
        }

        public DirectoryNode addDirectory(String name) {
            return addDirectory(name, 0);
        }

        public int size() {
            return getChildren().size();
        }

        public boolean isEmpty() {
            return getChildren().isEmpty();
        }

        public boolean contains(DataNode o) {
            return getChildren().contains(o);
        }

        @Override
        public Iterator<DataNode> iterator() {
            return getChildren().iterator();
        }

        public DataNode get(String name) {
            for (DataNode i : getChildren()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

        /**
         * called after modifying the list. It checks that names within the list
         * are unique, and that every item has this item as its parent
         *
         * @param newItem
         */
        private void checkConsistency(DataNode newItem) {
            if (members == null) {
                return; // nothing changed
            }
            Set<String> names = new HashSet<>();
            for (DataNode item : members) {
                if (names.contains(item.getName())) {
                    throw new RuntimeException("Found duplicate name in set: " + item.getName() + " when adding item: " + newItem.getName());
                }
                if (item.getParent() != this) {
                    throw new RuntimeException("Found item in this set which does not have this item as its parent: " + item.getName() + ". Its parent is: " + item.getParent().getName() + " and my name is : " + this.getName());
                }
            }
        }
    }

    public class FileNode extends DataNode {

        private Fanout fanout;

        public FileNode(DirectoryNode parent, String name, long hash) {
            super(parent, name, "f", hash);
        }

        @Override
        public void copy(DirectoryNode newDir, String newName) {
            newDir.addFile(newName, this.hash);
        }
        
        

        public void setContent(InputStream in) throws IOException {
            long oldhash = getHash();
            Parser parser = new Parser();
            long fileHash = parser.parse(in, hashStore, blobStore);
            setHash(fileHash);
        }

        private Fanout getFanout() {
            if (fanout == null) {
                fanout = hashStore.getFanout(getHash());
                if (fanout == null) {
                    throw new RuntimeException("Fanout not found: " + getHash());
                }
            }
            return fanout;
        }

        public void writeContent(OutputStream out) throws IOException {
            Combiner combiner = new Combiner();
            List<Long> fanoutCrcs = getFanout().getHashes();
            combiner.combine(fanoutCrcs, hashStore, blobStore, out);
            out.flush();
        }

        public long getContentLength() {
            return getFanout().getActualContentLength();
        }
    }
}
