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

import io.milton.cloud.server.db.Version;
import io.milton.cloud.server.web.ContentResource2;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.ProfileBean;
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
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.velocity.runtime.resource.ContentResource;
import org.hibernate.Session;

/**
 * Provides a JSON representation of the history of a resource
 *
 * @author brad
 */
public class HistoryResource implements GetableResource, DigestResource {

    private final String name;
    private final ContentResource2 contentResource;

    public HistoryResource(String name, ContentResource2 contentResource) {
        this.name = name;
        this.contentResource = contentResource;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        List<HistoryItem> history = new ArrayList<>();        
        Profile lastModifiedBy = contentResource.getModifiedBy();
        Version v = Version.find(contentResource.getHash(), contentResource.getModifiedDate(), lastModifiedBy.getId(), SessionManager.session());
        Session session = SessionManager.session();
        while( history.size() < 100 && v != null ) {
            history.add( toHistory(v));
            v = v.previousVersion(session);
        }
        JsonResult j = new JsonResult(true);
        j.setData(history);
        j.write(out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return JsonResult.CONTENT_TYPE;
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

    private HistoryItem toHistory(Version v) {
        Profile profile = Profile.get(v.getProfileId(), SessionManager.session());
        ProfileBean user = ProfileBean.toBean(profile);
        HistoryItem h = new HistoryItem();
        h.setUser(user);
        h.setModDate(v.getModDate().getTime());
        h.setDescription("Updated");
        return h;
    }

    public static class HistoryItem {
        private ProfileBean user;
        private long modDate;
        private String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getModDate() {
            return modDate;
        }

        public void setModDate(long modDate) {
            this.modDate = modDate;
        }

        public ProfileBean getUser() {
            return user;
        }

        public void setUser(ProfileBean user) {
            this.user = user;
        }
        
        
    }
}
