/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.user.UserDashboardApp;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.CookieAuthenticationHandler;
import io.milton.http.http11.auth.FormAuthenticationHandler;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.util.List;

/**
 *
 * @author brad
 */
public class SpliffyAjaxLoginResource extends AbstractResource implements GetableResource, PostableResource {

    private final RootFolder rootFolder;
    private final String name;
    private JsonResult jsonResult;

    public SpliffyAjaxLoginResource(RootFolder rootFolder, String name) {
        this.rootFolder = rootFolder;
        this.name = name;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
    }

    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        Request request = HttpManager.request();
        
        String loginResultAttName = _(FormAuthenticationHandler.class).getLoginResultAttName();
        // This tells us if an authentication was attempted, and if so what the result was
        Boolean loginResult = (Boolean) request.getAttributes().get(loginResultAttName);
        
        String userUrlAttName = _(CookieAuthenticationHandler.class).getUserUrlAttName();
        // This will be non-null if there is a current user, and may be used for redirecting to user's page
        String userUrl = (String) request.getAttributes().get(userUrlAttName);
        // Need to find out if user dashboard page is enabled. If so page will probably want to redirect there
        List<Application> apps = _(ApplicationManager.class).getActiveApps(rootFolder);
        boolean hasDashboard = false;
        if( rootFolder instanceof WebsiteRootFolder ) {
            hasDashboard = hasDashboardApp(apps);
        }
        
        jsonResult = new JsonResult();
        if( loginResult != null ) {
            jsonResult.setStatus(loginResult);
        }
        if( hasDashboard ) {            
            jsonResult.setNextHref(UserDashboardApp.DASHBOARD_HREF);
        }
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        if( p != null ) {
            jsonResult.setData( ProfileBean.toBean(p) );
        }
        
        jsonResult.write(out);
    }


    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        return null;
    }

    private boolean hasDashboardApp(List<Application> apps) {
        for( Application app : apps ) {
            if( app instanceof UserDashboardApp) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return rootFolder;
    }

    @Override
    public Organisation getOrganisation() {
        return rootFolder.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }
}
