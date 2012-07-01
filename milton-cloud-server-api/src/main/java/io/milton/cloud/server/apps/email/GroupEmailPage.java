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
package io.milton.cloud.server.apps.email;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.cloud.server.apps.signup.SignupPage;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Response.Status;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value="milton")
public class GroupEmailPage extends AbstractResource implements GetableResource, PostableResource, PropertySourcePatchSetter.CommitableResource {

    private static final Logger log = LoggerFactory.getLogger(SignupPage.class);
    
    private final CommonCollectionResource parent;
    private final GroupEmailJob job;

    public GroupEmailPage(GroupEmailJob job, CommonCollectionResource parent) {
        
        this.job = job;
        this.parent = parent;
    }

    public GroupEmailJob getJob() {
        return job;
    }   
    
    public Date getStatusDate() {
        return job.getStatusDate();
    }
            
    public String getTitle() {
        return job.getTitle();
    }
    
    public void setTitle(String s) {
        job.setTitle(s);
    }
    
    public String getNotes() {
        return job.getNotes();
    }
    
    public void setNotes(String s) {
        job.setNotes(s);
    }
    
    public String getFromAddress() {
        return job.getFromAddress();
    }
    
    public void setFromAddress(String s) {
        job.setFromAddress(s);
    }
    
    public String getSubject() {
        return job.getSubject();
    }
    
    public void setSubject(String s) {
        job.setSubject(s);
    }
    
    /**
     * returns the status code or "draft" if null
     * 
     * @return 
     */
    public String getStatusCode() {
        if( job.getStatus() == null || job.getStatus().length() == 0 ) {
            return "Draft";
        } else {
            switch( job.getStatus()) {
                case "c":
                    return "Completed";
                case "p":
                    return "Progress";
                default:
                    return "Status" + job.getStatus();
            }
        }
    }
    
    public String getStatus() {
        if( job.getStatus() == null || job.getStatus().length() == 0 ) {
            return "Draft";
        } else {
            switch( job.getStatus()) {
                case "c":
                    return "Completed";
                case "p":
                    return "In progress";
                default:
                    return "Status: " + job.getStatus();
            }
        }
    }
    
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
        
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {               
        MenuItem.setActiveIds("menuTalk", "menuEmails", "menuSendEmail");
        _(HtmlTemplater.class).writePage("email/groupEmailJob", this, params, out);
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
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return job.getName();
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
    
    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
        
    @Override
    public boolean is(String type) {
        if( type.equals("manageRewards")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        session.save(job);
        tx.commit();
    }
    
    
}
