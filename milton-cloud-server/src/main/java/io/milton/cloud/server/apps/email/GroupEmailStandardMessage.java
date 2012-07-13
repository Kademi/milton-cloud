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

import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.mail.Attachment;
import io.milton.mail.MailboxAddress;
import io.milton.mail.StandardMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Supports converting MimeMessage object to GroupEmailJobs
 *
 * @author brad
 */
public class GroupEmailStandardMessage implements StandardMessage{
    private final GroupEmailJob job;

    public GroupEmailStandardMessage(GroupEmailJob job) {
        this.job = job;
    }

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
    public String getSubject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MailboxAddress getFrom() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Attachment> getAttachments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFrom(MailboxAddress from) {
        job.setFromAddress(from.toPlainAddress());
    }

    @Override
    public MailboxAddress getReplyTo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setReplyTo(MailboxAddress replyTo) {
        job.setFromAddress(replyTo.toPlainAddress()); // HACK: should be replyto address
    }

    @Override
    public void setSubject(String subject) {
        job.setSubject(subject);
    }

    @Override
    public String getHtml() {
        return job.getHtml();
    }

    @Override
    public void setHtml(String html) {
        job.setHtml(html);
    }

    @Override
    public String getText() {
        return null;
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
        
    }

    @Override
    public String getDisposition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setEncoding(String encoding) {
        
    }

    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setContentLanguage(String contentLanguage) {
        
    }

    @Override
    public String getContentLanguage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String> getHeaders() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getTo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTo(List<MailboxAddress> to) {
        
    }

    @Override
    public List<MailboxAddress> getCc() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCc(List<MailboxAddress> cc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<MailboxAddress> getBcc() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBcc(List<MailboxAddress> bcc) {
        
    }

    @Override
    public StandardMessage instantiateAttachedMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
}
