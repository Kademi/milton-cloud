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
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.mail.Attachment;
import io.milton.mail.MailboxAddress;
import io.milton.mail.MessageResource;
import io.milton.mail.StandardMessage;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;

/**
 * Represents a single email, which might contain attachments
 *
 * @author brad
 */
public class EmailItemFolder extends AbstractCollectionResource implements GetableResource, MessageResource, StandardMessage {

    private final EmailFolder parent;
    private final EmailItem emailItem;
    private ResourceList children;

    public EmailItemFolder(EmailFolder parent, EmailItem emailItem) {
        
        this.parent = parent;
        this.emailItem = emailItem;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("email/myInbox", this, params, out);
    }

    public EmailFolder getInbox() {
        return parent;
    }

    public Date getDate() {
        return emailItem.getSendStatusDate();
    }

    public String getFromStr() {
        if (emailItem.getSender() != null) {
            String s = emailItem.getSender().getNickName();
            if (s == null) {
                s = emailItem.getSender().getName();
            }
            return s;
        } else {
            return emailItem.getFromAddress();
        }
    }

    @Override
    public String getSubject() {
        return emailItem.getSubject();
    }

    @Override
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
        if (children == null) {
            children = new ResourceList();
            _(ApplicationManager.class).addBrowseablePages(this, children);
            List<EmailItem> items = EmailItem.findByRecipient(getUser(), SessionManager.session());
            for (EmailItem item : items) {
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
        if (type.equals("message")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public void deleteMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeTo(OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Standard message interface
    @Override
    public void addAttachment(String name, String ct, String contentId, InputStream in) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<StandardMessage> getAttachedMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttachedMessages(List<StandardMessage> attachedMessages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MailboxAddress getFrom() {
        String s = getFromStr();
        return MailboxAddress.parse(s);
    }

    @Override
    public List<Attachment> getAttachments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFrom(MailboxAddress from) {
        this.emailItem.setFromAddress(from.toString());
    }

    @Override
    public MailboxAddress getReplyTo() {
        String s = emailItem.getReplyToAddress();
        return MailboxAddress.parse(s);
    }

    @Override
    public void setReplyTo(MailboxAddress replyTo) {
        emailItem.setReplyToAddress(replyTo.toString());
    }

    @Override
    public void setSubject(String subject) {
        emailItem.setSubject(subject);
    }

    @Override
    public void setHtml(String html) {
        emailItem.setHtml(html);
    }

    @Override
    public String getText() {
        return emailItem.getText();
    }

    @Override
    public void setText(String text) {
        emailItem.setText(text);
    }

    @Override
    public void setSize(int size) {
    //    emailItem.setSize(size);
    }

    @Override
    public void setDisposition(String disposition) {
        emailItem.setDisposition(disposition);
    }

    @Override
    public String getDisposition() {
        return emailItem.getDisposition();
    }

    @Override
    public void setEncoding(String encoding) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setContentLanguage(String contentLanguage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContentLanguage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
    }

    @Override
    public List<MailboxAddress> getTo() {
        List<MailboxAddress> list = new ArrayList<>();
        String[] arr = emailItem.getToList().split(";");
        for (String s : arr) {
            if (s != null && s.trim().length() > 0) {
                list.add(MailboxAddress.parse(s));
            }
        }
        return list;
    }

    @Override
    public void setTo(List<MailboxAddress> to) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getCc() {
        List<MailboxAddress> list = new ArrayList<>();
        String[] arr = emailItem.getCcList().split(";");
        for (String s : arr) {
            if (s != null && s.trim().length() > 0) {
                list.add(MailboxAddress.parse(s));
            }
        }
        return list;
    }

    @Override
    public void setCc(List<MailboxAddress> cc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getBcc() {
        List<MailboxAddress> list = new ArrayList<>();
        String[] arr = emailItem.getBccList().split(";");
        for (String s : arr) {
            if (s != null && s.trim().length() > 0) {
                list.add(MailboxAddress.parse(s));
            }
        }
        return list;
    }

    @Override
    public void setBcc(List<MailboxAddress> bcc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StandardMessage instantiateAttachedMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}