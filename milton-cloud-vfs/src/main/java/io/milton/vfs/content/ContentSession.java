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
package io.milton.vfs.content;

import io.milton.cloud.common.CurrentDateService;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.MetaItem;
import io.milton.vfs.meta.MetaSession;
import io.milton.vfs.meta.MetaSession.MetaNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.hashsplit4j.api.*;
import org.hibernate.Session;

/**
 * A content session brings together data and meta and allows them to be
 * manipulated in unison
 *
 * @author brad
 */
public class ContentSession {

    private final Session session;
    private final Branch branch;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private DataSession dataSession;
    private MetaSession metaSession;
    private CurrentDateService currentDateService;
    private DirectoryNode rootContentNode;
    private long currentUserId;

    public ContentSession(Session session, Branch branch, CurrentDateService currentDateService, long currentUserId, HashStore hashStore, BlobStore blobStore) {
        this.currentDateService = currentDateService;
        this.session = session;
        this.branch = branch;
        this.currentUserId = currentUserId;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        Commit c = branch.latestVersion(session);
        long hash = 0;
        if (c != null) {
            hash = c.getItemHash();
        }
        dataSession = new DataSession(hash, session);

        MetaItem rootMeta = branch.getRootMetaItem();
        metaSession = new MetaSession(rootMeta, session);

        rootContentNode = new DirectoryNode(null, dataSession.getRootDataNode(), metaSession.getRootNode());
    }

    public DirectoryNode getRootContentNode() {
        return rootContentNode;
    }

    public ContentNode find(Path path) {
        if (path.isRoot()) {
            return rootContentNode;
        } else {
            ContentNode parent = find(path.getParent());
            if (parent == null) {
                return null;
            } else {
                if (parent instanceof DirectoryNode) {
                    DirectoryNode d = (DirectoryNode) parent;
                    return d.child(path.getName());
                } else {
                    return null;
                }
            }
        }
    }

    public void save() {
        long newHash = dataSession.save();
        Commit newCommit = new Commit();
        newCommit.setCreatedDate(currentDateService.getNow());
        newCommit.setEditorId(currentUserId);
        newCommit.setItemHash(newHash);
        session.save(newCommit);
        branch.setHead(newCommit);
        session.save(branch);
    }

    public abstract class ContentNode {

        protected DirectoryNode parent;
        protected final DataSession.DataNode dataNode;
        protected MetaNode metaNode;
        protected List<ContentNode> children;

        private ContentNode(DirectoryNode parent, DataNode dataNode, MetaNode metaNode) {
            if (dataNode == null) {
                throw new IllegalArgumentException("dataNode is null");
            }
            this.parent = parent;
            this.dataNode = dataNode;
            this.metaNode = metaNode;
        }

        public String getName() {
            return dataNode.getName();
        }

        public void move(ContentNode newParent, String newName) {
            dataNode.move(newParent.dataNode, newName);
            metaNode.move(newParent.metaNode, newName);
        }

        public void delete() {
            dataNode.delete();
            metaNode.delete();
            if (parent.children != null) {
                parent.children.remove(this);
            }
        }
    }

    public class DirectoryNode extends ContentNode {

        public DirectoryNode(DirectoryNode parent, DataNode dataNode, MetaNode metaNode) {
            super(parent, dataNode, metaNode);
        }

        public Iterable<ContentNode> getChildren() {
            if (children == null) {
                children = new ArrayList<>();
                for (DataNode childNode : dataNode) {
                    MetaNode m = metaNode.get(childNode.getName(), session);
                    ContentNode c;
                    if (childNode.getType().equals("d")) {
                        c = new DirectoryNode(this, childNode, m);
                    } else {
                        c = new FileNode(this, childNode, m);
                    }
                    children.add(c);
                }
            }
            return children;
        }

        public ContentNode child(String name) {
            for (ContentNode i : getChildren()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

        public DirectoryNode addDirectory(String name) {
            DataNode newDataNode = dataNode.add(name, 0, "d");
            MetaNode newMetaNode = metaNode.add(name, session);
            DirectoryNode newContentNode = new DirectoryNode(this, newDataNode, newMetaNode);
            getChildren();
            children.add(newContentNode);
            return newContentNode;
        }

        public FileNode addFile(String name) {
            DataNode newDataNode = dataNode.add(name, 0, "f");
            MetaNode newMetaNode = metaNode.add(name, session);
            FileNode newContentNode = new FileNode(this, newDataNode, newMetaNode);
            getChildren();
            children.add(newContentNode);
            return newContentNode;
        }
    }

    public class FileNode extends ContentNode {

        private Fanout fanout;
        
        public FileNode(DirectoryNode parent, DataNode dataNode, MetaNode metaNode) {
            super(parent, dataNode, metaNode);
        }

        public void setHash(long hash) {
            dataNode.setHash(hash);
            metaNode.setModifiedDate(currentDateService.getNow());
        }
        
        public long getHash() {
            return dataNode.getHash();
        }

        public void setContent(InputStream in) throws IOException {
            Parser parser = new Parser();
            long fileHash;
            fileHash = parser.parse(in, hashStore, blobStore);
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
    }
}
