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
import io.milton.vfs.db.MetaItem;
import java.util.Date;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class MetaSession {

    private MetaNode rootNode;
    private Session session;

    public MetaSession(MetaItem treeItem, Session session) {
        this.rootNode = new MetaNode(null, treeItem);
        this.session = session;
    }

    public MetaNode getRootNode() {
        return rootNode;
    }

    public MetaNode get(Path path) {
        if (path.isRoot()) {
            return rootNode;
        } else {
            MetaNode parent = get(path.getParent());
            if (parent == null) {
                return null;
            } else {
                return parent.get(path.getName(), session);
            }
        }
    }

    public class MetaNode {

        private final MetaItem metaItem;
        private MetaNode parent;

        public MetaNode(MetaNode parent, MetaItem treeItem) {
            if(treeItem == null ) {
                throw new IllegalArgumentException("MetaItem cannot be null");
            }
            this.metaItem = treeItem;
            this.parent = parent;
        }

        public MetaItem getTreeItem() {
            return metaItem;
        }

        public Date getCreatedDate() {
            return metaItem.getCreatedDate();
        }

        public Date getModifiedDate() {
            return metaItem.getModifiedDate();
        }

        public void setModifiedDate(Date d) {
            metaItem.setModifiedDate(d);
            session.save(metaItem);
        }

        public MetaNode getParent() {
            return parent;
        }

        public void setParent(MetaNode parent) {
            this.parent = parent;
        }

        public String getName() {
            return metaItem.getName();
        }

        public void setName(String s) {
            metaItem.setName(s);
        }

        public MetaNode get(String name, Session session) {
            MetaItem childSource = metaItem;
            while (childSource.getRelatedTo() != null) {
                childSource = childSource.getRelatedTo();
            }
            MetaItem child = childSource.getDirectChild(name, session);
            if (child != null) {
                return new MetaNode(this, metaItem);
            } else {
                return null;
            }
        }

        public void move(MetaNode newParent, String name) {
            MetaNode oldParent = this.getParent();
            if (oldParent != newParent) {
                this.setParent(newParent);
            }
            if (name.equals(getName())) {
                setName(name);
            }
        }

        public MetaNode add(String name, Session session) {
            MetaItem newChild = new MetaItem();
            newChild.setName(name);
            newChild.setCreatedDate(new Date());
            newChild.setModifiedDate(new Date());
            newChild.setParent(metaItem);
            session.save(newChild);
            return new MetaNode(this, metaItem);
        }

        public void delete() {
            metaItem.setDeletedFromParent(metaItem.getParent());
            metaItem.setParent(null);
            session.save(metaItem);
        }
    }
}
