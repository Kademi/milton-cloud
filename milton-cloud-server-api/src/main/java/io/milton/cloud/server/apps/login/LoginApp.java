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
package io.milton.cloud.server.apps.login;

import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.web.*;

/**
 *
 * @author brad
 */
public class LoginApp implements MenuApplication{

    private Services services;
    
    @Override
    public Resource getPage(Resource parent, String childName) {
        if( parent instanceof RootFolder) {            
            RootFolder rf = (RootFolder) parent;
            if( childName.equals("login.html")) {
                return new LoginPage(services.getSecurityManager(), rf); 
            }            
        }
        return null;
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.services = resourceFactory.getServices();
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        
    }

    @Override
    public String getInstanceId() {
        return "login"; // only single instance
    }


    @Override
    public void appendMenu(MenuItem parent) {
        // TODO: login and logout users menu
    }



    
}
