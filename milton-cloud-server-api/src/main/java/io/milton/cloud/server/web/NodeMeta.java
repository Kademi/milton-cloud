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
package io.milton.cloud.server.web;

import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.data.DataSession.FileNode;
import java.io.*;
import java.util.Date;
import java.util.UUID;

/**
 * NoteMeta represents data stored in a special directory inside vfs
 * directories, holding meta information about files and folders
 *
 * @author brad
 */
public class NodeMeta {

    public static NodeMeta loadForNode(DataNode node) throws IOException {
        DirectoryNode parent = node.getParent();
        if (parent == null) {
            return new NodeMeta(null, null, null, 0);
        }
        DataNode metaDir = parent.get(".mil");
        if (metaDir == null) {
            return new NodeMeta(null, null, null, 0);
        }
        String metaName = node.getName() + ".meta";
        if (metaDir instanceof DirectoryNode) {
            DirectoryNode metaDirNode = (DirectoryNode) metaDir;
            DataNode metaNode = metaDirNode.get(metaName);
            if (metaNode instanceof FileNode) {
                FileNode metaFileNode = (FileNode) metaNode;
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                metaFileNode.writeContent(bout);
                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                DataInputStream din = new DataInputStream(bin);

                long mostSigBits = 0;
                long leastSigBits = 0;
                long lMod = 0;
                long lCreated = 0;
                long profileId = 0;

                try {
                    mostSigBits = din.readLong();
                    leastSigBits = din.readLong();
                    lMod = din.readLong();
                    lCreated = din.readLong();
                    profileId = din.readLong();
                } catch (EOFException e) {
                }

                UUID id = new UUID(mostSigBits, leastSigBits);
                Date mod = new Date(lMod);
                Date created = new Date(lCreated);
                return new NodeMeta(mod, created, id, profileId);
            } else {
                return new NodeMeta(null, null, null, 0);
            }
        } else {
            return new NodeMeta(null, null, null, 0);
        }
    }

    public static void saveMeta(DataNode node, NodeMeta nodeMeta) throws IOException {
        DirectoryNode parent = node.getParent();
        if (parent == null) {
            return;// can't save
        }
        DataNode metaDir = parent.get(".mil");
        if (metaDir == null) {
            metaDir = parent.addDirectory(".mil");
        }
        if (metaDir instanceof DirectoryNode) {
            String metaName = node.getName() + ".meta";
            DirectoryNode metaDirNode = (DirectoryNode) metaDir;
            DataNode metaNode = metaDirNode.get(metaName);
            if (metaNode == null) {
                metaNode = metaDirNode.addFile(metaName);
            }
            if (metaNode instanceof FileNode) {
                FileNode metaFileNode = (FileNode) metaNode;
                UUID id = nodeMeta.getId();
                if (id == null) {
                    id = UUID.randomUUID();
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeLong(id.getMostSignificantBits());
                dout.writeLong(id.getLeastSignificantBits());
                dout.writeLong(nodeMeta.modDate.getTime());
                dout.writeLong(nodeMeta.createdDate.getTime());
                dout.writeLong(nodeMeta.profileId);
                InputStream in = new ByteArrayInputStream(bout.toByteArray());
                metaFileNode.setContent(in);
            } else {
                return; // can't save
            }
        } else {
            return; // can't save
        }
    }
    private Date modDate;
    private Date createdDate;
    private long profileId;
    private final UUID id;

    public NodeMeta(Date modDate, Date createdDate, UUID id, long profileId) {
        this.modDate = modDate;
        this.createdDate = createdDate;
        this.id = id;
        this.profileId = profileId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * This is intended to uniquely identify a resource, but note that if a
     * folder is copied the meta data will also be copied, so the id's will not
     * be unique
     *
     * @return
     */
    public UUID getId() {
        return id;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }
}
