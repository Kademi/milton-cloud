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

import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Display a list of commits for a branch
 *
 * @author brad
 */
public class CommitsResource extends AbstractResource implements GetableResource, PostableResource {

    private final String name;
    private final BranchFolder parent;
    

    public CommitsResource(String name, BranchFolder parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getTitle() {
        return parent.getName() + " Commits";
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuDashboard", "menuFileManager", "menuManageRepos");
        _(HtmlTemplater.class).writePage("fileserver/commits", this, params, out);
    }
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<BranchFolder> getCommits() {
        List<BranchFolder> list = new ArrayList<>();
        for( Commit c : Commit.findByBranch(parent.getBranch(), SessionManager.session())) {
            String commitName = "commit-" + c.getId();
            list.add( new BranchFolder(commitName, parent.getParent(), c));
        }
        return list;
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
