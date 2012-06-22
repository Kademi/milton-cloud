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

import io.milton.vfs.content.ContentSession;
import io.milton.vfs.content.ContentSession.ContentNode;
import io.milton.vfs.content.ContentSession.DirectoryNode;
import io.milton.vfs.content.ContentSession.FileNode;

/**
 *
 * @author brad
 */
public class NodeChildUtils {

    /**
     * Produce a web resource representation of the given ItemHistory.
     *
     * This will be either a FileResource or a DirectoryResource, depending on
     * the type associated with the member
     *
     * @param parent
     * @param dm
     * @return
     */
    public static CommonResource toResource(ContentDirectoryResource parent, ContentNode contentNode, boolean renderMode, ResourceCreator resourceCreator) {
        if (contentNode instanceof ContentSession.DirectoryNode) {
            DirectoryNode dm = (DirectoryNode) contentNode;
            DirectoryResource rdr = resourceCreator.newDirectoryResource(dm, parent, renderMode);
            return rdr;
        } else if (contentNode instanceof ContentSession.FileNode) {
            FileNode dm = (FileNode) contentNode;
            FileResource rfr = resourceCreator.newFileResource(dm, parent, renderMode);
            if (renderMode) {
                if (isHtml(rfr)) {
                    return new RenderFileResource(rfr);
                }
                return rfr;
            } else {
                return rfr;
            }
        } else {
            throw new RuntimeException("Unknown resource type: " + contentNode);
        }
    }

    public static boolean isHtml(FileResource rfr) {
        String ct = rfr.getContentType("text/html"); // find if it can produce html
        return "text/html".equals(ct);
    }

    public static ResourceList toResources(ContentDirectoryResource parent, ContentSession.DirectoryNode dir, boolean renderMode, ResourceCreator resourceCreator) {
        ResourceList list = new ResourceList();
        for (ContentSession.ContentNode n : dir.getChildren()) {
            CommonResource r = toResource(parent, n, renderMode, resourceCreator);
            list.add(r);
        }
        return list;
    }

    public interface ResourceCreator {

        FileResource newFileResource(FileNode dm, ContentDirectoryResource parent, boolean renderMode);

        DirectoryResource newDirectoryResource(DirectoryNode dm, ContentDirectoryResource parent, boolean renderMode);
    }
}
