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

import io.milton.cloud.server.web.FileResource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.*;
import java.util.Objects;

/**
 * Provides access to a template stored in the content repository
 *
 * @author brad
 */
public class ContentTemplateHtmlPage extends TemplateHtmlPage{

    private final byte[] data;
    private final long timestamp;
    private final long hash;
    
    
    public ContentTemplateHtmlPage(FileResource fr) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        super(fr.getHref());
        timestamp = System.currentTimeMillis();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        fr.sendContent(bout, null, null, "text/html");
        data = bout.toByteArray();
        hash = fr.getHash();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public long getHash() {
        return hash;
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
            return (other.hash == hash);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(hash);
        return hash;
    }

    @Override
    boolean isValid() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
