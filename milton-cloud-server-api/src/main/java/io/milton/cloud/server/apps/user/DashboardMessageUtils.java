/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.common.CurrentDateService;
import io.milton.http.Request;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.utils.SessionManager;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;

import static io.milton.context.RequestContext._;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author brad
 */
public class DashboardMessageUtils {

    public static void saveProps(Properties props, Request request, Group group, Branch b) throws IOException {
        DataSession.FileNode fileNode = htmlFileNode(request, group, b);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        props.store(bout, null);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        fileNode.setContent(bin);
    }

    public static Properties messageProps(Request request, Group group, Branch b) throws IOException {
        String key = "_dashmsg_filenode_" + group.getId() + "_" + b.getId();
        Properties props = (Properties) request.getAttributes().get(key);
        if (props == null) {
            props = new Properties();
            DataSession.FileNode fileNode = htmlFileNode(request, group, b);
            if (fileNode.getHash() != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                fileNode.writeContent(bout);
                props.load(new ByteArrayInputStream(bout.toByteArray()));
            }
        }
        return props;
    }

    public static DataSession.FileNode htmlFileNode(Request request, Group group, Branch b) {
        String key = "_dashmsg_filenode_" + group.getId() + "_" + b.getId();
        DataSession.FileNode _htmlFileNode = (DataSession.FileNode) request.getAttributes().get(key);
        if (_htmlFileNode == null) {
            String fileName = group.getName() + ".properties";
            _htmlFileNode = (DataSession.FileNode) htmlDirNode(request, group, b).get(fileName);
            if (_htmlFileNode == null) {
                _htmlFileNode = htmlDirNode(request, group, b).addFile(fileName);
            }
        }
        return _htmlFileNode;
    }

    public static DataSession.DirectoryNode htmlDirNode(Request request, Group group, Branch b) {
        String key = "_dashmsg_dir_" + group.getId() + "_" + b.getId();
        DataSession.DirectoryNode _htmlDirNode = (DataSession.DirectoryNode) request.getAttributes().get(key);
        if (_htmlDirNode == null) {
            _htmlDirNode = (DataSession.DirectoryNode) dataSession(request, group, b).getRootDataNode().get("dashboardMessages");
            if (_htmlDirNode == null) {
                _htmlDirNode = dataSession(request, group, b).getRootDataNode().addDirectory("dashboardMessages");
            }
        }
        return _htmlDirNode;
    }

    public static DataSession dataSession(Request request, Group group, Branch b) {
        String key = "_dashmsg_session_" + group.getId() + "_" + b.getId();
        DataSession _dataSession = (DataSession) request.getAttributes().get(key);
        if (_dataSession == null) {
            _dataSession = new DataSession(b, SessionManager.session(), _(HashStore.class), _(BlobStore.class), _(CurrentDateService.class));
            request.getAttributes().put(key, _dataSession);
        }
        return _dataSession;
    }
}
