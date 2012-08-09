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
package io.milton.cloud.server.apps.dynamiccss;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.web.*;
import io.milton.common.Path;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.Date;

/**
 *
 * @author brad
 */
public class DynamicCssApp implements ResourceApplication {

    private SpliffyResourceFactory resourceFactory;

    private Date modDate;

    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Allows CSS files to have parameterised which can be configured through the website";
    }
    
    
    
    @Override
    public Resource getResource(RootFolder webRoot, String path) {
        if (!path.endsWith(".dyn.css")) {
            return null;
        }

        Path p = Path.path(path);
   
        TemplatedTextPage t = new TemplatedTextPage(p.getName(), webRoot, "text/css", path);
        t.setModifiedDate(modDate);
        return t;
    }

    @Override
    public String getInstanceId() {
        return "dynamicCss";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.resourceFactory = resourceFactory;
        modDate = config.getContext().get(CurrentDateService.class).getNow();
    }

    @Override
    public Resource getPage(Resource parent, String path) {
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }
}
