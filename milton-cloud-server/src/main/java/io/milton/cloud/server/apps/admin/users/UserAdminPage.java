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
package io.milton.cloud.server.apps.admin.users;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.cloud.server.apps.signup.SignupPage;
import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.db.Profile;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.cloud.server.db.utils.UserDao;
import io.milton.cloud.server.web.*;
import io.milton.http.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.acl.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class UserAdminPage extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(SignupPage.class);
    
    private final String name;
    private final SpliffyCollectionResource parent;
    private final Organisation organisation;
    private JsonResult jsonResult;
    private List<Profile> searchResults;

    public UserAdminPage(String name, Organisation organisation, SpliffyCollectionResource parent, Services services) {
        super(services);
        this.organisation = organisation;
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {        
        UserDao userDao = services.getSecurityManager().getUserDao();        
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        Organisation org = rootFolder.getOrganisation();
        if( params.containsKey("q")) {
            String q = params.get("q");
//            searchResults = userDao.search(q, org); // find the given user in this organisation
            // todo
        } else {
            searchResults = userDao.listProfiles(org, SessionManager.session()); // find the given user in this organisation
        }
        services.getHtmlTemplater().writePage("manageUsers/userAdmin", this, params, out);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        System.out.println("Child: " + childName + " parent: " + organisation.getName());
        Organisation childOrg = OrganisationDao.getOrganisation(organisation, childName, SessionManager.session());
        if( childOrg != null ) {
            System.out.println("foudn it");
            return new UserAdminPage(childName, childOrg, this, services);
        }
        System.out.println("not found");
        return null;
    }
    
    
    
    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public SpliffyCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }


    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    public List<Profile> getSearchResults() {
        return searchResults;
    }
    
    @Override
    public Organisation getOrganisation() {
        return organisation;
    }
    
    public List<Organisation> getChildOrganisations() {
        List<Organisation> list = new ArrayList<>();        
        List<BaseEntity> members = getOrganisation().getMembers();
        if( members == null || members.isEmpty() ) {
            return Collections.EMPTY_LIST;
        }
        for( BaseEntity be : members ) {
            if( be instanceof Organisation) {
                list.add((Organisation)be);
            }
        }
        return list;
    }

    @Override
    public boolean is(String type) {
        if( type.equals("userAdmin")) {
            return true;
        }
        return super.is(type);
    }
    
}
