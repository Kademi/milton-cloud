/*
 * Copyright 2013 McEvoy Software Ltd.
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
package io.milton.cloud.server.apps.cors;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.WebUtils;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.GetEvent;
import io.milton.http.AbstractResponse;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See: http://en.wikipedia.org/wiki/Cross-Origin_Resource_Sharing
 *
 * @author brad
 */
public class CorsApp implements Application, EventListener{

    private static final Logger log = LoggerFactory.getLogger(CorsApp.class);
    
    private ApplicationManager applicationManager;
    
    @Override
    public String getInstanceId() {
        return "CORS";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, GetEvent.class);
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Cross-origin resource sharing";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "When enabled, other websites are permitted to access content within this website(s)";
    }

    @Override
    public void onEvent(Event e) {
        if( e instanceof GetEvent) {
            Request req = HttpManager.request();            
            if( req == null ) {
                return ;
            }
            String origin = req.getOriginHeader();
            if( origin == null ) {
                return ; // not relevant
            }            
            GetEvent ge = (GetEvent) e;
            // ok, lets check to see if this app is enabled
            RootFolder rf = WebUtils.findRootFolder(ge.getResource());
            if( rf instanceof WebsiteRootFolder) {
                WebsiteRootFolder wrf = (WebsiteRootFolder) rf;
                if( applicationManager.isActive(this, wrf.getBranch())) {
                    // enabled, so we'll let anyone
                    Response resp = HttpManager.response();
                    if( resp != null ) {
                        if( log.isTraceEnabled()) {
                            log.trace("permit CORS request from: " + origin);
                        }
                        resp.setAccessControlAllowOrigin(origin);
                    }
                }
            }
        }
    }
    
}
