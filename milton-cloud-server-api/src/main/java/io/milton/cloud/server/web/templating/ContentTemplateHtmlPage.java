/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.templating;

import io.milton.cloud.server.DataSessionManager;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.NodeChildUtils;
import io.milton.cloud.server.web.RootFolder;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.*;
import java.util.Objects;

import static io.milton.context.RequestContext._;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;

/**
 * Provides access to a template stored in the content repository
 *
 * @author brad
 */
public class ContentTemplateHtmlPage extends TemplateHtmlPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContentTemplateHtmlPage.class);
    private final byte[] data;
    private final String loadedHash;
    private final Long branchId;
    private final Path path;

    public ContentTemplateHtmlPage(DataSession.FileNode fileNode, Branch branch) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        super("fileNode:" + fileNode.getHash());
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        fileNode.writeContent(bout);
        data = bout.toByteArray();
        loadedHash = fileNode.getHash();
        this.branchId = branch.getId();
        this.path = NodeChildUtils.getPath(fileNode);
    }

    @Override
    public long getTimestamp() {
        DataSession.FileNode node = getCurrentFileResource();
        if (node == null) {
            return -1;
        } else {
            return node.getHash().hashCode();
        }
    }
    
    @Override
    public boolean isValid() {
        if (loadedHash == null) {
            throw new RuntimeException("loadedHash is null");
        }
        DataSession.FileNode node = getCurrentFileResource();
        if (node == null) {
            return false;
        } else {
            return loadedHash.equals(node.getHash());
        }
    }    

    @Override
    public String getSource() {
        return "fileRes-" + getHash();
    }

    public DataSession.FileNode getCurrentFileResource() {
        DataSession dataSession = _(DataSessionManager.class).get(branchId);
        DataSession.DataNode dataNode = NodeChildUtils.find(dataSession, path);
        if (dataNode instanceof DataSession.FileNode) {
            return (FileNode) dataNode;
        } else {
            return null;
        }
    }

    public String getHash() {
        return loadedHash;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ContentTemplateHtmlPage) {
            ContentTemplateHtmlPage other = (ContentTemplateHtmlPage) obj;
            return (other.loadedHash.equals(loadedHash));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 3;
        h = 67 * h + Objects.hashCode(h);
        return h;
    }
}
