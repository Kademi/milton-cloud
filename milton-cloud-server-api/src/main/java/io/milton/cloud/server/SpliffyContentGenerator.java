/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server;

import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.common.Path;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.Response.Status;
import io.milton.http.http11.ContentGenerator;
import io.milton.resource.Resource;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author brad
 */
public class SpliffyContentGenerator implements ContentGenerator {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffyContentGenerator.class);
    
    public SpliffyContentGenerator() {
    }

    
    
    @Override
    public void generate(Resource resource, Request request, Response response, Status status) {
        log.error("Showing error page for: " + request.getAbsolutePath());
        try {
            CurrentRootFolderService currentRootFolderService = _(CurrentRootFolderService.class);
            RootFolder rf = currentRootFolderService.getRootFolder(request.getHostHeader());
            Resource r = new ErrorResource(rf, status.code + "", status.text);
            _(HtmlTemplater.class).writePage("error/" + r.getName(), r, null, response.getOutputStream());
        } catch (IOException ex) {
            log.error("Exception sending error page", ex);
        }
    }

    public class ErrorResource implements CommonResource {

        private final String errorCode;
        private final String errorText;
        private final RootFolder rootFolder;

        public ErrorResource(RootFolder rootFolder, String errorCode, String errorText) {
            this.rootFolder = rootFolder;
            this.errorCode = errorCode;
            this.errorText = errorText;
        }
 
        
        @Override
        public String getUniqueId() {
            return null;
        }

        @Override
        public String getName() {
            return errorCode;
        }

        @Override
        public Object authenticate(String user, String password) {
            return user;
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return true;
        }

        @Override
        public String getRealm() {
            return "error";
        }

        @Override
        public Date getModifiedDate() {
            return null;
        }

        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            return null;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorText() {
            return errorText;
        }

        @Override
        public CommonCollectionResource getParent() {
            return rootFolder;
        }

        @Override
        public Organisation getOrganisation() {
            return null;
        }

        @Override
        public boolean is(String type) {
            return type.equals("error");
        }

        @Override
        public Path getPath() {
            return Path.root.child(getName());
        }

        @Override
        public boolean isPublic() {
            return true;
        }

        @Override
        public Priviledge getRequiredPostPriviledge(Request request) {
            return null;
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return digestRequest;
        }

        @Override
        public boolean isDigestAllowed() {
            return true;
        }                        
    }
}
