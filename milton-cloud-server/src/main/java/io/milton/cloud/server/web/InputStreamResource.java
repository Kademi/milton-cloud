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
package io.milton.cloud.server.web;


import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author brad
 */
public class InputStreamResource implements GetableResource, DigestResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(InputStreamResource.class);
    private final String name;
    private final String realm;
    private final InputStream content;
    private final Long maxAgeSecs;

    public InputStreamResource(String name, InputStream content, Long maxAgeSecs, String realm) {
        this.name = name;
        this.content = content;
        this.maxAgeSecs = maxAgeSecs;
        this.realm = realm;
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        try {
            IOUtils.copy(content, out);
        } catch (NullPointerException npe) {
            log.debug("NullPointerException, this is often expected");
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return maxAgeSecs;
    }

    @Override
    public String getContentType(String preferredList) {
        String mime = ContentTypeUtils.findContentTypes(name);
        String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
        return s;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object authenticate(String user, String password) {
        return user;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        return null;
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return digestRequest.getUser();
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

}
