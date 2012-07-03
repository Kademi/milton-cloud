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

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class SignupPage extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(SignupPage.class);
    
    private final String name;
    private final WebsiteRootFolder parent;
    private JsonResult jsonResult;

    public SignupPage(String name, WebsiteRootFolder parent) {       
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("signup", this, params, out);
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        // ajax request to signup
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            String newName = parameters.get("name");
            if( newName == null || newName.trim().length() == 0 ) {
                throw new Exception("No name was provided");
            }
            Profile u = new Profile();
            u.setOrganisation(parent.getOrganisation());
            u.setName(newName);
            u.setEmail(parameters.get("email"));
            u.setCreatedDate(new Date());
            u.setModifiedDate(new Date());
            session.save(u);

            String password = parameters.get("password");
            if( password == null || password.trim().length() == 0 ) {
                throw new Exception("No password given");
            }
            _(SpliffySecurityManager.class).getPasswordManager().setPassword(u, password);
            
            addCalendar("cal", u, session);     
            addAddressBook("contact", u, session);         
            addRepo("Documents", u, session);
            addRepo("Music", u, session);
            addRepo("Pictures", u, session);
            addRepo("Videos", u, session);
                       
            tx.commit();

            String userPath = "/" + u.getName(); // todo: encoding
            jsonResult = new JsonResult(true, "Created account", userPath);
        } catch (Exception e) {
            log.error("Exception creating user", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }

    private void addCalendar(String name, Profile u, Session session) throws HibernateException {
        Calendar cal = new Calendar();
        cal.setOwner(u);
        cal.setCreatedDate(new Date());
        cal.setCtag(System.currentTimeMillis());
        cal.setModifiedDate(new Date());
        cal.setName(name);
        session.save(cal);
    }

    private void addAddressBook(String name, Profile u, Session session) throws HibernateException {
        AddressBook addressBook = new AddressBook();
        addressBook.setName(name);
        addressBook.setOwner(u);
        addressBook.setCreatedDate(new Date());
        addressBook.setModifiedDate(new Date());
        addressBook.setDescription("My contacts");
        session.save(addressBook);
    }

    private void addRepo(String name, Profile u, Session session) throws HibernateException {
        Repository r1 = new Repository();
        r1.setBaseEntity(u);
        r1.setCreatedDate(new Date());
        r1.setName(name);            
        session.save(r1);
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
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        list.add(Priviledge.READ);
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
    
    
}
