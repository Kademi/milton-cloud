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
import io.milton.mail.Attachment;
import io.milton.mail.MailboxAddress;
import io.milton.mail.StandardMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author brad
 */


public class EmailItemStandardMessage implements StandardMessage{

    private final EmailItem emailItem;

    public EmailItemStandardMessage(EmailItem emailItem) {
        this.emailItem = emailItem;
    }
        
    
    @Override
    public void addAttachment(String name, String ct, String contentId, InputStream in) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<StandardMessage> getAttachedMessages() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void setAttachedMessages(List<StandardMessage> attachedMessages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSubject() {
        return emailItem.getSubject();
    }

    @Override
    public MailboxAddress getFrom() {
        return MailboxAddress.parse(emailItem.getFromAddress());
    }

    @Override
    public List<Attachment> getAttachments() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void setFrom(MailboxAddress from) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MailboxAddress getReplyTo() {
        return MailboxAddress.parse(emailItem.getReplyToAddress());
    }

    @Override
    public void setReplyTo(MailboxAddress replyTo) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSubject(String subject) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHtml() {
        return emailItem.getHtml();
    }

    @Override
    public void setHtml(String html) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getText() {
        return emailItem.getText();
    }

    @Override
    public void setText(String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSize(int size) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDisposition(String disposition) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDisposition() {
        return null;
    }

    @Override
    public void setEncoding(String encoding) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public void setContentLanguage(String contentLanguage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContentLanguage() {
        return null;
    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getTo() {
        if( emailItem.getToList() == null ) {
            return Collections.EMPTY_LIST;
        }
        String[] arr = emailItem.getToList().split(",");                
        List<MailboxAddress> list = new ArrayList<>();
        for( String s : arr ) {
            s = s.trim();
            list.add(MailboxAddress.parse(s));
        }
        return list;
    }

    @Override
    public void setTo(List<MailboxAddress> to) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getCc() {
        if( emailItem.getCcList() == null ) {
            return Collections.EMPTY_LIST;
        }
        String[] arr = emailItem.getCcList().split(",");                
        List<MailboxAddress> list = new ArrayList<>();
        for( String s : arr ) {
            s = s.trim();
            list.add(MailboxAddress.parse(s));
        }
        return list;

    }

    @Override
    public void setCc(List<MailboxAddress> cc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getBcc() {
        if( emailItem.getBccList() == null ) {
            return Collections.EMPTY_LIST;
        }
        String[] arr = emailItem.getBccList().split(",");                
        List<MailboxAddress> list = new ArrayList<>();
        for( String s : arr ) {
            s = s.trim();
            list.add(MailboxAddress.parse(s));
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
