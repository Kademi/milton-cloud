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
package io.milton.cloud.server.apps.groupresources;

import io.milton.cloud.server.apps.website.ManageWebsitesFolder;
import io.milton.cloud.server.apps.admin.*;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.resource.CollectionResource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ManageGroupResourcesPage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebsitesFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private JsonResult jsonResult;

    public ManageGroupResourcesPage(String name, CommonCollectionResource parent) {
        this.parent = parent;
        this.name = name;
    }

    public String getTitle() {
        return "Manage group resources";
    }

    /**
     * Take the parameter "createItemId" and look up a GroupInWebsiteItem from getGroupFolders
     * 
     * If one is found then attempt to create its DirectoryResource, if it doesnt
     * already exist. Set the JsonResult object indicating if it was successful, 
     * and if so if it was created or already existing, and return the href of the
     * directory
     * 
     * @param parameters
     * @param files
     * @return
     * @throws BadRequestException
     * @throws NotAuthorizedException
     * @throws ConflictException 
     */
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        String createItemId = WebUtils.getRawParam(parameters, "createItemId");
        if( createItemId != null ) {
            OrganisationFolder orgFolder = WebUtils.findParentOrg(this);
            List<GroupInWebsiteItem> list = getGroupFolders(orgFolder);
            for( GroupInWebsiteItem giwi : list ) {
                if( createItemId.equals(giwi.id)) {
                    if( giwi.directory != null ) {
                        jsonResult = new JsonResult(true, "Already exists", giwi.directory.getHref());                        
                    } else {
                        Session session = SessionManager.session();
                        Transaction tx = session.beginTransaction();
                        DirectoryResource dir = findDirectory(giwi.giw, orgFolder, true, session);
                        try {
                            dir.save();
                            tx.commit();
                            jsonResult = new JsonResult(true, "Created", dir.getHref());                            
                        } catch (IOException ex) {
                            log.error("ioex", ex);
                            jsonResult = new JsonResult(false, "Exception occured, please try again");
                        }                        
                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin", "groupresources/manageGroupResources", this, params, out);
        }
    }

    public List<GroupInWebsiteItem> getGroupFolders() throws NotAuthorizedException, BadRequestException {
        OrganisationFolder orgFolder = WebUtils.findParentOrg(this);
        return getGroupFolders(orgFolder);
    }
    
    public List<GroupInWebsiteItem> getGroupFolders(OrganisationFolder orgFolder) throws NotAuthorizedException, BadRequestException {
        List<GroupInWebsiteItem> list = new ArrayList<>();
        if (getOrganisation().getWebsites() != null) {
            for (Website w : getOrganisation().getWebsites()) {
                for (GroupInWebsite giw : GroupInWebsite.findByWebsite(w, SessionManager.session())) {
                    GroupInWebsiteItem item = new GroupInWebsiteItem();
                    item.id = giw.getWebsite().getName() + "_" + giw.getUserGroup().getName();
                    item.giw = giw;
                    DirectoryResource dir = findDirectory(giw, orgFolder, false, SessionManager.session());                    
                    item.directory = dir;
                    list.add(item);
                }
            }
        }
        return list;
    }    

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
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

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    private DirectoryResource findDirectory(GroupInWebsite giw, OrganisationFolder orgFolder, boolean autoCreate, Session session) throws NotAuthorizedException, BadRequestException {
        Resource r = orgFolder.find("websites/" + giw.getWebsite().getName());
        if( r == null ) {
            throw new RuntimeException("Couldnt find website directory: " + giw.getWebsite().getName());
        }
        if( r instanceof CollectionResource ) {
            CollectionResource col = (CollectionResource) r; // should be repo folder
            r = col.child(Branch.TRUNK);
            if( r instanceof BranchFolder ) {
                BranchFolder branch = (BranchFolder) r; // branch
                DirectoryResource dirRes = branch.getOrCreateDirectory("resources", autoCreate);
                if( dirRes != null ) {
                    DirectoryResource dirGroup = dirRes.getOrCreateDirectory(giw.getUserGroup().getName(), autoCreate);
                    if( dirGroup == null && autoCreate ) {
                        throw new RuntimeException("Failed to get or create a directory when autoCreate is true");
                    }
                    return dirGroup;
                }
            }
        }
        log.warn("Couldnt find or create directory because parent is not a collection: " + r);
        return null;
    }

    public class GroupInWebsiteItem {

        private String id;
        private GroupInWebsite giw;
        private DirectoryResource directory;

        public String getId() {
            return id;
        }
        
        public Group getGroup() {
            return giw.getUserGroup();
        }
        
        public Website getWebsite() {
            return giw.getWebsite();
        }

        public DirectoryResource getDirectory() {
            return directory;
        }                
    }
}
