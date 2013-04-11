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
import io.milton.cloud.server.mail.BatchEmailService;
import io.milton.context.RootContext;
import io.milton.mail.StandardMessageFactory;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.store.mail.MailStore;

/**
 *
 * @author brad
 */
public class EmailItemMailStore implements MailStore {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmailItemMailStore.class);
    private final SessionManager sessionManager;
    private final StandardMessageFactory standardMessageFactory;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final RootContext rootContext;
    private AspirinInternal aspirinInternal;

    public EmailItemMailStore(SessionManager sessionManager, StandardMessageFactory standardMessageFactory, HashStore hashStore,BlobStore blobStore,RootContext rootContext) {
        this.sessionManager = sessionManager;
        this.standardMessageFactory = standardMessageFactory;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.rootContext = rootContext;
    }

    @Override
    public MimeMessage get(String mailid) {
        Session session = sessionManager.open();
        try {
            Long id = Long.parseLong(mailid);
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            if (i == null) {
                return null;
            }
            try {
                MimeMessage msg = toMimeMessage(i);
                return msg;
            } catch (Throwable e) {
                log.error("Failed to retrieve message: " + mailid + " marking message as failed", e);
                Transaction tx = session.beginTransaction();
                i.setSendStatus("f");
                i.setSendStatusDate(new Date());
                session.save(i);
                tx.commit();
                return null;
            }
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
        EmailItemStandardMessage sm = new EmailItemStandardMessage(i, hashStore, blobStore, rootContext, sessionManager);
        MimeMessage mm = aspirinInternal.createNewMimeMessage();
        System.out.println("EmailItemMailStore - toMimeMessage - atts=" + sm.getAttachments().size() + " - " + standardMessageFactory.getClass());
        standardMessageFactory.toMimeMessage(sm, mm);
        System.out.println("EmailItemMailStore - toMimeMessage - done");
        return mm;
    }

    public AspirinInternal getAspirinInternal() {
        return aspirinInternal;
    }

    public void setAspirinInternal(AspirinInternal aspirinInternal) {
        this.aspirinInternal = aspirinInternal;
    }
}
