/*
 * Copyright 2013 McEvoy Software Ltd.
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
package io.milton.cloud.server.text;

import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author brad
 */
public class TextGenResource extends AbstractResource implements GetableResource {
    private final CommonCollectionResource parent;
    private final Resource base;
    private final String name;

    public TextGenResource(CommonCollectionResource parent, Resource base, String name) {
        this.parent = parent;
        this.base = base;
        this.name = name;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        String text = getText();
        out.write(text.getBytes("UTF-8"));
        out.flush();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/plain; charset=utf-8";
    }

    /**
     * Get the HTML of the base resource
     * @return 
     */
    private String getText() {
        StringBuilder sb = new StringBuilder();
        try {
            appendText(sb, base);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
        return sb.toString();
    }
    
    private void appendText(StringBuilder sb, Resource r)throws NotAuthorizedException, BadRequestException  {
        if( base instanceof RenderFileResource) {
            appendText(sb, (RenderFileResource)base);
        } else if( base instanceof FileResource) {
            appendText(sb, (FileResource)base);
        } else if( base instanceof CommonCollectionResource) {
            CommonCollectionResource col = (CommonCollectionResource) base;
            appendText(sb, col);
        }        
    }

    private void appendText(StringBuilder sb, CommonCollectionResource r) throws NotAuthorizedException, BadRequestException {        
        for( Resource child : r.getChildren()) {
            appendText(sb, child);
        }
    }    
    
    private String appendText(StringBuilder sb, RenderFileResource r) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private String appendText(StringBuilder sb, FileResource r) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    
    
    
    
    
}
