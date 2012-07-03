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

import io.milton.cloud.server.apps.website.WebsiteApp;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.NodeChildUtils;
import io.milton.common.Path;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.*;
import java.util.Objects;

import static io.milton.context.RequestContext._;
import io.milton.resource.Resource;


/**
 * Provides access to a template stored in the content repository
 *
 * @author brad
 */
public class ContentTemplateHtmlPage extends TemplateHtmlPage{

    private final byte[] data;
    private final long loadedHash;
    private String websiteName;
    private final Path path;
    
    
    public ContentTemplateHtmlPage(FileResource fr, String websiteName, Path path) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        super(fr.getHref());
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        fr.sendContent(bout, null, null, "text/html");
        data = bout.toByteArray();
        loadedHash = fr.getHash();
        this.websiteName = websiteName;
        this.path = path;
    }

    @Override
    public long getTimestamp() {
        FileResource fr = getCurrentFileResource();
        if( fr == null ) {
            return -1;
        } else {
            return fr.getHash();
        }
    }
    
    public FileResource getCurrentFileResource() {
        WebsiteRootFolder wrf = _(WebsiteApp.class).getPage(null, websiteName);
        if( wrf == null ) {
            return null;
        }
        try {
            Resource r = NodeChildUtils.find(path, wrf);
            FileResource fr = NodeChildUtils.toFileResource(r);
            return fr;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }    

    public long getHash() {
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
            return (other.loadedHash == loadedHash);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 3;
        h = 67 * h + Objects.hashCode(h);
        return h;
    }

    @Override
    boolean isValid() {
        return loadedHash == getTimestamp();
    }
}
