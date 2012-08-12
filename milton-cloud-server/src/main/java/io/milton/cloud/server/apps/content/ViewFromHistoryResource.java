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
package io.milton.cloud.server.apps.content;

import io.milton.cloud.server.web.ContentResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author brad
 */
public class ViewFromHistoryResource implements GetableResource, DigestResource {

    private final String name;
    private final ContentResource contentResource;

    public ViewFromHistoryResource(String name, ContentResource contentResource) {
        this.name = name;
        this.contentResource = contentResource;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (params.containsKey("version")) {
            String previewHash = params.get("version");
            if (contentResource instanceof GetableResource) {
                contentResource.setHash(previewHash);
                GetableResource gr = (GetableResource) contentResource;
                gr.sendContent(out, range, params, contentType);
            }
        }
    }

    @Override
    public String getContentType(String accepts) {
        if (contentResource instanceof GetableResource) {
            GetableResource gr = (GetableResource) contentResource;
            return gr.getContentType(accepts);
        } else {
            return null;
        }
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
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object authenticate(String user, String password) {
        return contentResource.authenticate(user, password);
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return contentResource.authorise(request, method, auth);
    }

    @Override
    public String getRealm() {
        return contentResource.getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return contentResource.getModifiedDate();
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return contentResource.authenticate(digestRequest);
    }

    @Override
    public boolean isDigestAllowed() {
        return contentResource.isDigestAllowed();
    }
}
