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
import io.milton.mail.StandardMessageFactory;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.hibernate.Session;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.mail.MailStore;

/**
 *
 * @author brad
 */
public class EmailItemMailStore implements MailStore{

    private final SessionManager sessionManager;
    private final StandardMessageFactory standardMessageFactory;
    private AspirinInternal aspirinInternal;

    public EmailItemMailStore(SessionManager sessionManager, StandardMessageFactory standardMessageFactory) {
        this.sessionManager = sessionManager;
        this.standardMessageFactory = standardMessageFactory;
    }
        
    
    @Override
    public MimeMessage get(String mailid) {
        Session session = sessionManager.open();
        try {
            Long id = Long.parseLong(mailid);
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            if( i == null ) {
                return null;
            }
            MimeMessage msg = toMimeMessage(i);
            return msg;
        } finally {
            sessionManager.close();
        }
    }

    @Override
    public List<String> getMailIds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() {
        
    }

    @Override
    public void remove(String mailid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void set(String mailid, MimeMessage msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private MimeMessage toMimeMessage(EmailItem i) {
        EmailItemStandardMessage sm = new EmailItemStandardMessage(i);
        MimeMessage mm = aspirinInternal.createNewMimeMessage();
        standardMessageFactory.toMimeMessage(sm, mm);
        return mm;
    }

    public AspirinInternal getAspirinInternal() {
        return aspirinInternal;
    }

    public void setAspirinInternal(AspirinInternal aspirinInternal) {
        this.aspirinInternal = aspirinInternal;
    }
    
    
    
}
