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
package io.milton.cloud.server.apps.signup;

import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;

/**
 * Manages registration of a user when signing up to a group
 *
 * @author brad
 */
public class GroupRegistrationPage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(GroupRegistrationPage.class);
    
    private final String name;
    private final GroupInWebsiteFolder parent;
    private JsonResult jsonResult;

    public GroupRegistrationPage(String name, GroupInWebsiteFolder parent) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {        
        if (jsonResult != null) {
            log.info("sendContent: json");
            jsonResult.write(out);
        } else {
            log.info("sendContent: render page");
            _(HtmlTemplater.class).writePage("signup/register", this, params, out);
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        // ajax request to signup
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            Organisation org = parent.getOrganisation();
            
            String nickName = WebUtils.getParam(parameters,"nickName");
            if( nickName != null ) {
                nickName = nickName.trim();
                if( nickName.length() == 0 ) {
                    nickName = null;
                }
            }
            
            String newName = WebUtils.getParam(parameters,"name");
            if( newName == null || newName.trim().length() == 0 ) {
                if( nickName == null  ) {
                    jsonResult = JsonResult.fieldError("nickName", "Please enter a name or nick name");
                    return null;
                }
                newName = org.findUniqueName(nickName, session);
            }
            
            // Check email is unique within the org
            String email = WebUtils.getParam(parameters,"email");
            Profile p = Profile.findByEmail(email, org, session);
            if( p != null ) {
                jsonResult = JsonResult.fieldError("email", "There is already a user registered with that email");
                return null;
            }
            
            Profile u = new Profile();
            u.setOrganisation(org);
            u.setName(newName);
            u.setNickName(nickName);
            u.setEmail(email);
            u.setCreatedDate(new Date());
            u.setModifiedDate(new Date());
            session.save(u);

            String password = parameters.get("password");
            if( password == null || password.trim().length() == 0 ) {
                throw new Exception("No password given");
            }
            _(SpliffySecurityManager.class).getPasswordManager().setPassword(u, password);

            u.addToGroup(parent.getGroup(), parent.getOrganisation());
            RootFolder rf = WebUtils.findRootFolder(this);
            _(SignupApp.class).onNewProfile(u, rf);
                                   
            tx.commit();

            String userPath = "/" + u.getName(); // todo: encoding
            log.info("Created user: " + userPath);
            jsonResult = new JsonResult(true, "Created account", userPath);
        } catch (Exception e) {
            log.error("Exception creating user", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        if (jsonResult != null) {
            return "application/x-javascript; charset=utf-8";
        }
        return "text/html";
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
    }

    @Override
    public boolean isDir() {
        return false;
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
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean isPublic() {
        return true;
    }
    
    
}
