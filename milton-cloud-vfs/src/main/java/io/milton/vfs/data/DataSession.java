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

import io.milton.cloud.common.HashCalc;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.common.ITriplet;
import io.milton.common.Path;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import org.hashsplit4j.api.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(DataSession.class);
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
        String hash = null;
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

    public boolean dirExists(String hash) {
        return blobStore.hasBlob(hash);
    }

    public List<ITriplet> find(String hash) {
        //return DataItem.findByHash(hash, session);
        byte[] arr = blobStore.getBlob(hash);
        if (arr == null) {
            return null;
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(arr);
        try {
            return hashCalc.parseTriplets(bin);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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
    public String save(Profile currentUser) throws IOException {
        recalcHashes(rootDataNode);

        String newHash = rootDataNode.hash;

        Commit newCommit = new Commit();
        newCommit.setCreatedDate(currentDateService.getNow());
        newCommit.setEditor(currentUser);
        newCommit.setItemHash(newHash);
        session.save(newCommit);
        System.out.println("commit hash: " + newHash + " id " + newCommit.getId());
        branch.setHead(newCommit);
        session.save(branch);

        return rootDataNode.hash;
    }

    private void recalcHashes(DataNode item) throws IOException {
        if (item.dirty == null) {
            return; // not dirty, which means no children are dirty
        }
        // only directories have derived hashes        
        if (item instanceof DirectoryNode) {
            DirectoryNode dirNode = (DirectoryNode) item;
            for (DataNode child : dirNode) {
                recalcHashes(child);
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            hashCalc.sort(dirNode.getChildren());
            String newHash = hashCalc.calcHash(dirNode, bout);
            item.setHash(newHash);
            byte[] arrTriplets = bout.toByteArray();
            blobStore.setBlob(newHash, arrTriplets);
            log.info("recalcHashes: " + item.name + " children:" + dirNode.members.size() + " hash=" + newHash);
            System.out.println(bout.toString());
            log.info("--- End " + item.name);

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
        protected String hash;
        protected String loadedHash; // holds the hash value from when the node was loaded
        protected Boolean dirty;

        /**
         * Copy just creates the same type of item with the same hash
         *
         * @param newDir
         * @param newName
         */
        public abstract void copy(DirectoryNode newDir, String newName);

        private DataNode(DirectoryNode parent, String name, String type, String hash) {
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
        public void move(DirectoryNode newParent, String newName) {
            DirectoryNode oldParent = this.getParent();
            if (oldParent != newParent) {
                this.setParent(newParent);
                if (oldParent.members != null) {
                    oldParent.members.remove(this);
                }
                newParent.getChildren().add(this);
                setDirty();
                newParent.setDirty();
                oldParent.setDirty();
            }
            if (!newName.equals(name)) {
                setName(newName);
            }
            parent.checkConsistency(this);
        }

        public void delete() {
            throw new RuntimeException("DELETE");
//            parent.getChildren().remove(this);
//            parent.checkConsistency(parent);
//            setDirty();
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
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            log.info("setHash: " + hash + " on " + getName());
            this.hash = hash;
            setDirty();
        }

        public String getLoadedHash() {
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

        public DirectoryNode(DirectoryNode parent, String name, String hash) {
            super(parent, name, "d", hash);
        }

        @Override
        public void copy(DirectoryNode newDir, String newName) {
            newDir.addDirectory(newName, this.hash);
        }

        private List<DataNode> getChildren() {
            if (members == null) {
                System.out.println("Load Children for: " + name + " hash=" + hash);
                members = new ArrayList<>();
                if (hash != null) {
                    List<ITriplet> list = find(hash);
                    if (list != null) {
                        for (ITriplet i : list) {
                            DataNode c;
                            if (i.getType().equals("d")) {
                                c = new DirectoryNode(this, i.getName(), i.getHash());
                            } else {
                                c = new FileNode(this, i.getName(), i.getHash());
                            }
                            members.add(c);
                        }
                    }
                }
                log.info("DirectoryNode: loaded children for " + getName() + " = " + members.size() + " from hash: " + hash);
            }
            return members;
        }

        public FileNode addFile(String name) {
            return addFile(name, null);
        }

        public FileNode addFile(String name, String hash) {
            log.info("addFile: " + name + " - " + hash);
            FileNode item = new FileNode(this, name, hash);
            getChildren().add(item);
            checkConsistency(item);
            setDirty();
            return item;
        }

        public DirectoryNode addDirectory(String name, String hash) {
            DirectoryNode item = new DirectoryNode(this, name, hash);
            getChildren().add(item);
            setDirty();
            return item;
        }

        public DirectoryNode addDirectory(String name) {
            return addDirectory(name, null);
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
                    if (newItem != null) {
                        throw new RuntimeException("Found duplicate name: " + item.getName() + " when adding item: " + newItem.getName() + " to directory: " + getName());
                    } else {
                        throw new RuntimeException("Found duplicate name: " + item.getName() + " in parent: " + getName());
                    }
                }
                if (item.getParent() != this) {
                    throw new RuntimeException("Found item in this set which does not have this item as its parent: " + item.getName() + ". Its parent is: " + item.getParent().getName() + " and my name is : " + this.getName());
                }
                names.add(item.getName());
            }
        }
    }

    public class FileNode extends DataNode {

        private Fanout fanout;

        public FileNode(DirectoryNode parent, String name, String hash) {
            super(parent, name, "f", hash);
        }

        @Override
        public void copy(DirectoryNode newDir, String newName) {
            newDir.addFile(newName, this.hash);
        }

        public void setContent(InputStream in) throws IOException {
            Parser parser = new Parser();
            String fileHash = parser.parse(in, hashStore, blobStore);
            setHash(fileHash);
        }

        private Fanout getFanout() {
            if (fanout == null) {
                fanout = hashStore.getFileFanout(getHash());
                if (fanout == null) {
                    throw new RuntimeException("Fanout not found: " + getHash());
                }
            }
            return fanout;
        }

        public void writeContent(OutputStream out) throws IOException {
            Combiner combiner = new Combiner();
            List<String> fanoutCrcs = getFanout().getHashes();
            combiner.combine(fanoutCrcs, hashStore, blobStore, out);
            out.flush();
        }

        /**
         * Write partial content, only
         *
         * @param out
         * @param start
         * @param finish
         * @throws IOException
         */
        public void writeContent(OutputStream out, long start, Long finish) throws IOException {
            Combiner combiner = new Combiner();
            List<String> fanoutCrcs = getFanout().getHashes();
            combiner.combine(fanoutCrcs, hashStore, blobStore, out, start, finish);
            out.flush();
        }

        public long getContentLength() {
            return getFanout().getActualContentLength();
        }
    }
}
