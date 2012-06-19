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

import io.milton.cloud.common.ITriplet;
import io.milton.common.Path;
import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.DataItem;
import java.util.*;
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

    private DataNode rootDataNode;
    private final Session session;
    private HashCalc hashCalc = HashCalc.getInstance();

    public DataSession(long hash, Session session) {
        this.session = session;
        rootDataNode = new DataNode(null, null, "d", hash);        
    }

    public DataNode getRootDataNode() {
        return rootDataNode;
    }
    

    public DataNode find(Path path) {
        if (path.isRoot()) {
            return rootDataNode;
        } else {
            DataNode parent = find(path.getParent());
            if (parent == null) {
                return null;
            } else {
                return parent.get(path.getName());
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
    public long save() {
        recalcHashes(rootDataNode);

        // persist stuff
        saveDir(rootDataNode);

        //DirectMeta meta = (DirectMeta) rootVfsItem.getMeta();
        //return Arrays.asList(meta.getTreeItem());
        return rootDataNode.hash;
    }

    private void recalcHashes(DataNode item) {
        if (item.dirty == null) {
            return; // not dirty, which means no children are dirty
        }
        // only directories have derived hashes
        if (!item.getType().equals("d")) {
            return;
        }
        for (DataNode child : item) {
            recalcHashes(child);
        }
        long newHash = hashCalc.calcHash(item);
        item.setHash(newHash);
    }

    private void saveDir(DataNode item) {
        if (item.dirty == null) {
            return; // not dirty, which means no children are dirty
        }
        // the directory list to save might already be saved, in which case
        // unique constraints will prevent the insertion, so check first
        List<DataItem> existing = find(item.hash);
        if( existing != null && !existing.isEmpty()) {
            return ; // already in db
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
        if (item.getType().equals("d")) {
            saveDir(item);
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
    public class DataNode implements Iterable<DataNode>, ITriplet {

        private DataNode parent;
        private String name;
        private String type;
        private long hash;
        private List<DataNode> members;
        private Boolean dirty;

        private DataNode(DataNode parent, String name, String type, long hash) {
            this.parent = parent;
            this.name = name;
            this.type = type;
            this.hash = hash;
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
         * Move this item to a new parent, or to a new name, or both
         *
         * @param newParent
         */
        public void move(DataNode newParent, String name) {
            DataNode oldParent = this.getParent();
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
            checkConsistency(this);
        }

        private List<DataNode> getChildren() {
            if (members == null) {
                List<DataItem> list = find(hash);
                members = new ArrayList<>();
                for (DataItem i : list) {
                    DataNode vfsItem = new DataNode(this, i.getName(), i.getType(), i.getItemHash());                    
                    members.add(vfsItem);
                }
            }
            return members;
        }

        public DataNode add(String name, long hash, String type) {
            DataNode item = new DataNode(this, name, type, hash);
            getChildren().add(item);
            setDirty();
            return item;
        }

        public void delete() {
            parent.getChildren().remove(this);
            checkConsistency(parent);
            setDirty();
        }

        public DataNode getParent() {
            return parent;
        }

        private void setParent(DataNode parent) {
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

        private void setDirty() {
            if (dirty != null) {
                return; // already set
            }
            dirty = Boolean.TRUE;
            if (parent != null) {
                parent.setDirty();
            }
        }
    }
}
