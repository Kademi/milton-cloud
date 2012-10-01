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

import io.milton.common.ContentTypeUtils;
import io.milton.common.FileUtils;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.DataNode;
import io.milton.vfs.data.DataSession.DirectoryNode;
import io.milton.vfs.data.DataSession.FileNode;
import java.util.List;

/**
 * Just shove stuff in here that doesnt fit anywhere else. Hopefuly mostly to do
 * with nodes and children
 *
 * @author brad
 */
public class NodeChildUtils {


    public static boolean isHtml(FileResource rfr) {
        String ct = rfr.getUnderlyingContentType();
        return ct != null && ct.contains("html");
    }


    public static Resource childOf(List<? extends Resource> children, String name) {
        if (children == null) {
            return null;
        }
        for (Resource r : children) {
            if (r.getName().equals(name)) {
                return r;
            }
        }
        return null;
    }

    public static Resource find(Path p, CollectionResource col) throws NotAuthorizedException, BadRequestException {
        Resource r = col;
        for (String s : p.getParts()) {
            if (r instanceof CollectionResource) {
                r = ((CollectionResource) r).child(s);
            } else {
                return null;
            }
        }
        return r;
    }

    public static String getHash(Resource r) {
        if (r instanceof FileResource) {
            FileResource fr = (FileResource) r;
            return fr.getHash();
        } else if (r instanceof RenderFileResource) {
            RenderFileResource rfr = (RenderFileResource) r;
            return getHash(rfr.getFileResource());
        } else {
            return null;
        }
    }

    public static FileResource toFileResource(Resource r) {
        if (r instanceof FileResource) {
            FileResource fr = (FileResource) r;
            return fr;
        } else if (r instanceof RenderFileResource) {
            RenderFileResource rfr = (RenderFileResource) r;
            return rfr.getFileResource();
        } else {
            return null;
        }
    }

    public static String findName(String baseName, String defaultName, DataSession.DirectoryNode dir) {
        if (baseName == null || baseName.length() == 0) {
            baseName = defaultName;
        } else {
            if (baseName.contains("\\")) {
                baseName = baseName.substring(baseName.lastIndexOf("\\"));
            }
        }
        String candidateName = baseName;
        int cnt = 1;
        while (contains(dir, candidateName)) {
            candidateName = baseName + cnt++;
        }
        return candidateName;
    }    
    
    public static boolean contains(DataSession.DirectoryNode dir, String name) {
        for (DataNode n : dir) {
            if (n.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }    
}
