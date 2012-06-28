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
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a single email, which might contain attachments
 *
 * @author brad
 */
public class EmailItemFolder extends AbstractCollectionResource implements GetableResource {
    private final EmailFolder parent;
    private final EmailItem emailItem;
    
    private ResourceList children;

    public EmailItemFolder(EmailFolder parent, EmailItem emailItem) {
        super(parent.getServices());
        this.parent = parent;
        this.emailItem = emailItem;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        services.getHtmlTemplater().writePage("email/myInbox", this, params, out);
    }
    
    public EmailFolder getInbox() {
        return parent;
    }
    
    public Date getDate() {
        return emailItem.getSendStatusDate();
    }
    
    public String getFrom() {
        if( emailItem.getSender() != null ) {
            String s = emailItem.getSender().getNickName();
            if( s == null ) {
                s = emailItem.getSender().getName();
            }
            return s;
        } else {
            return emailItem.getFromAddress();
        }
    }
    
    public String getSubject() {
        return emailItem.getSubject();
    }
    
    public String getHtml() {
        return emailItem.getHtml();
    }
        
    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return getParent().getOwner();
    }

    @Override
    public void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user) {
        getParent().addPrivs(list, user);
    }

    @Override
    public String getName() {
        return emailItem.getId() + "";
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ResourceList();
            services.getApplicationManager().addBrowseablePages(this, children);
            List<EmailItem> items = EmailItem.findByRecipient(getUser(), SessionManager.session());
            for( EmailItem item : items ) {
                
            }
        }
        return children;
    }
    
    public Profile getUser() {
        return parent.getUser();
    }
    

    
    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
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
        if( type.equals("message")) {
            return true;
        }
        return super.is(type);
    }
    
    
 
}