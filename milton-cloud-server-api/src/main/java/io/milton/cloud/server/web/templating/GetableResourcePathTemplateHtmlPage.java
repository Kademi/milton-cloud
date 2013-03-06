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

import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.common.Path;
import io.milton.context.ClassNotInContextException;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import java.io.*;
import java.util.Objects;

import static io.milton.context.RequestContext._;
import io.milton.resource.Resource;
import java.util.Date;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author brad
 */
public class GetableResourcePathTemplateHtmlPage extends TemplateHtmlPage {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GetableResourcePathTemplateHtmlPage.class);

    public static String getId(String host, String path) {
        return "res://" + host + "/" + path;
    }
    private static final Path webPath = Path.path("/theme");
    private byte[] bytes;
    private final String host;
    private final Path path;
    private final long defaultTimestamp;
    private final HtmlTemplateParser templateParser;

    public GetableResourcePathTemplateHtmlPage(String host, String path, long defaultTimestamp, HtmlTemplateParser templateParser) {
        super(getId(host, path));
        this.defaultTimestamp = defaultTimestamp;
        this.host = host;
        this.path = Path.path(path);
        this.templateParser = templateParser;
    }

    @Override
    public long getTimestamp() {
        Resource r = findResource();
        if (r == null) {
            return defaultTimestamp;
        } else {
            Date dt = r.getModifiedDate();
            if (dt == null) {
                return defaultTimestamp;
            } else {
                return dt.getTime();
            }
        }
    }

    @Override
    public String getSource() {
        return getId();
    }

    public void parse() throws IOException, XMLStreamException {
        GetableResource resource = findResource();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            log.warn("parse: " + resource.getClass());
            resource.sendContent(bout, null, null, null);
        } catch (Throwable e) {
            throw new RuntimeException("Couldnt parse: " + resource.getClass(), e);
        }
        bytes = bout.toByteArray();
        templateParser.parse(this, path.getParent());
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof GetableResourcePathTemplateHtmlPage) {
            GetableResourcePathTemplateHtmlPage other = (GetableResourcePathTemplateHtmlPage) obj;
            return (other.getSource().equals(getSource()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSource());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public GetableResource findResource() {
        try {
            Resource r = _(SpliffyResourceFactory.class).findResource(host, path);
            if (r instanceof RenderFileResource) {
                RenderFileResource rfr = (RenderFileResource) r;
                return rfr.getFileResource();
            } else if (r instanceof GetableResource) {
                GetableResource gr = (GetableResource) r;
                return gr;
            } else if (r != null) {
                throw new RuntimeException("Template resource is not getable: " + r.getClass());
            } else {
                return null;
            }
        } catch (ClassNotInContextException | NotAuthorizedException | BadRequestException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getPath() {
        return path;
    }
    
    
}
