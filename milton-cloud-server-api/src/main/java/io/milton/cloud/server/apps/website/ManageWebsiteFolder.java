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
package io.milton.cloud.server.apps.website;

import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ContentRedirectorPage;
import io.milton.cloud.server.web.RepositoryFolder;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;

/**
 * A website is a specialisation of a repository. But most work is done on 
 * a branch within a website
 *
 * @author brad
 */
public class ManageWebsiteFolder extends RepositoryFolder implements WebsiteResource {

    public ManageWebsiteFolder(CommonCollectionResource parent, Repository r) {
        super(parent, r);
    }
    
    @Override
    public Website getWebsite() {
        return (Website) repo;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        ContentRedirectorPage.select(this);
        super.sendContent(out, range, params, contentType);
    }
        
    @Override
    public boolean is(String type) {
        if( type.equals("website")) {
            return true;
        }
        return super.is(type);
    }
    
    public String getExternalUrl() {
        return "http://" + getWebsite().getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain() + _(Formatter.class).getPortString() + "/";
    }
}
