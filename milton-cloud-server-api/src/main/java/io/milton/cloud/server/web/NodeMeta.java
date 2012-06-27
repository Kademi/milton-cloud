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
 *
 * @author brad
 */
public class NodeMeta {
    
    public static NodeMeta loadForNode(DataNode node) throws IOException {
        System.out.println("loadForNote: " + node.getName());
        DirectoryNode parent = node.getParent();
        if( parent == null ) {
            System.out.println(" - loadForNode: parent is null");
            return new NodeMeta(null, null, null);
        }
        DataNode metaDir = parent.get(".mil");
        if( metaDir == null) {
            System.out.println(" - loadForNote: metadir is null: " + parent.getName());
            return new NodeMeta(null, null, null);
        }
        String metaName = node.getName() + ".meta";
        if( metaDir instanceof DirectoryNode) {
            DirectoryNode metaDirNode = (DirectoryNode) metaDir;
            DataNode metaNode = metaDirNode.get(metaName);
            if( metaNode instanceof FileNode) {
                FileNode metaFileNode = (FileNode) metaNode;
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                metaFileNode.writeContent(bout);
                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                DataInputStream din = new DataInputStream(bin);
                long mostSigBits = din.readLong();
                long leastSigBits = din.readLong();
                UUID id = new UUID(mostSigBits, leastSigBits);
                long lMod = din.readLong();
                long lCreated = din.readLong();
                Date mod = new Date(lMod);
                Date created = new Date(lCreated);
                return new NodeMeta(mod, created, id);
            } else {
                System.out.println(" - loadForNote: metaNode is not a file node");
                return new NodeMeta(null, null, null);
            }
        } else {
            System.out.println(" - loadForNote: metaDir is not a directory");
            return new NodeMeta(null, null, null);
        }
    }
    
    public static void saveMeta(DataNode node, NodeMeta nodeMeta) throws IOException {
        DirectoryNode parent = node.getParent();
        if( parent == null ) {
            return ;// can't save
        }
        DataNode metaDir = parent.get(".mil");
        if( metaDir == null) {
            metaDir = parent.addDirectory(".mil");
        }
        if( metaDir instanceof DirectoryNode) {
            String metaName = node.getName() + ".meta";
            DirectoryNode metaDirNode = (DirectoryNode) metaDir;
            DataNode metaNode = metaDirNode.get(metaName);
            if( metaNode == null ) {
                metaNode = metaDirNode.addFile(metaName);
            }            
            if( metaNode instanceof FileNode) {
                FileNode metaFileNode = (FileNode) metaNode;
                UUID id = nodeMeta.getId();
                if( id == null ) {
                    id = UUID.randomUUID();
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeLong(id.getMostSignificantBits());
                dout.writeLong(id.getLeastSignificantBits() );
                dout.writeLong(nodeMeta.modDate.getTime());
                dout.writeLong(nodeMeta.createdDate.getTime());
                InputStream in = new ByteArrayInputStream(bout.toByteArray());
                metaFileNode.setContent(in);
            } else {
                return ; // can't save
            }
        } else {
            return ; // can't save
        }        
    }
    
    private Date modDate;
    private Date createdDate;
    private final UUID id;
    private FileNode loadedFrom;

    public NodeMeta(Date modDate, Date createdDate, UUID id) {
        this.modDate = modDate;
        this.createdDate = createdDate;
        this.id = id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public UUID getId() {
        return id;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }
    
    
    
}
