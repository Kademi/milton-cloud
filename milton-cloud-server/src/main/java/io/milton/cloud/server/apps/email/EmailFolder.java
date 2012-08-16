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

import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.mail.MessageFolder;
import io.milton.mail.MessageResource;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;

/**
 * List emails for this user
 *
 * @author brad
 */
public class EmailFolder extends AbstractCollectionResource implements GetableResource, MessageFolder {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmailFolder.class);
    private final CommonCollectionResource parent;
    private final String name;
    private BaseEntity baseEntity;
    private ResourceList children;

    public EmailFolder(CommonCollectionResource parent, String name, BaseEntity baseEntity) {
        this.parent = parent;
        this.name = name;
        this.baseEntity = baseEntity;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveId("menuNotifications");
        _(HtmlTemplater.class).writePage("email/myInbox", this, params, out);
    }

    public BaseEntity getBaseEntity() {
        return baseEntity;
    }

    
    
    public EmailFolder getInbox() {
        return this;
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
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        System.out.println("EmailFolder: child: " + childName);
        Long id = Long.parseLong(childName);
        EmailItem item = (EmailItem) SessionManager.session().get(EmailItem.class, id);
        if (item != null) {
            return new EmailItemFolder(this, item);
        }

        return super.child(childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        System.out.println("getchildren - " + baseEntity);
        if (children == null) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
            List<EmailItem> items = EmailItem.findByRecipient(getEntity(), SessionManager.session());
            for (EmailItem item : items) {
                EmailItemFolder f = new EmailItemFolder(this, item);
                children.add(f);
            }
        }
        return children;
    }

    public BaseEntity getEntity() {
        if (baseEntity == null) {
            baseEntity = _(SpliffySecurityManager.class).getCurrentUser();
        }
        return baseEntity;
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
    public Collection<MessageResource> getMessages() {
        try {
            Collection<MessageResource> col = new ArrayList<>();
            for (Resource r : getChildren()) {
                if (r instanceof MessageResource) {
                    col.add((MessageResource) r);
                }
            }
            return col;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int numMessages() {
        return getMessages().size();
    }

    @Override
    public int totalSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        log.info("authorise: " + auth);
        if (auth == null) {
            return false;
        }
        UserResource r = (UserResource) auth.getTag();
        if (r == null) {
            return false;
        }
        // The user always has read/write access to their own email
        if (r.getThisUser() == getEntity()) {
            return true;
        }
        System.out.println("this user: " + r.getThisUser().getName() + " - " + r.getThisUser().getId());
        System.out.println("baseentity: " + baseEntity.getName() + " - " + baseEntity.getId());
        // Not this user, so check permissions to see if admin
        log.info("not owning user, check sec manager");
        return super.authorise(request, method, auth);
    }
}