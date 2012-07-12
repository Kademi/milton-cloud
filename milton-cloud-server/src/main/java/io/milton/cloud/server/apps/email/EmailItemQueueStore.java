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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.EmailSendAttempt;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.listener.ListenerManager;
import org.masukomi.aspirin.core.store.queue.DeliveryState;
import org.masukomi.aspirin.core.store.queue.QueueInfo;
import org.masukomi.aspirin.core.store.queue.QueueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class EmailItemQueueStore implements QueueStore {

    private static final Logger log = LoggerFactory.getLogger(EmailItemQueueStore.class);
    private final SessionManager sessionManager;
    private final Configuration aspirinConfiguration;
    private final ListenerManager listenerManager;
    private final CurrentDateService currentDateService;
    private List<QueueInfo> currentQueue;
    private long retryIntervalMs = 60 * 1000l; // 60secs
    private int maxAttempts = 5;

    public EmailItemQueueStore(SessionManager sessionManager, Configuration aspirinConfiguration, ListenerManager listenerManager, CurrentDateService currentDateService) {
        this.sessionManager = sessionManager;
        this.aspirinConfiguration = aspirinConfiguration;
        this.listenerManager = listenerManager;
        this.currentDateService = currentDateService;
    }

    @Override
    public void add(String mailid, long expire, Collection<InternetAddress> recipients) throws MessagingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> clean() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueueInfo createQueueInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getNextAttempt(String mailid, String recipient) {
        log.info("getNextAttempt: " + mailid + " - " + recipient);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasBeenRecipientHandled(String mailid, String recipient) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() {
        log.info("init");
    }

    @Override
    public boolean isCompleted(String mailid) {
        log.info("isCompleted: " + mailid);
        Session session = sessionManager.open();
        try {
            Long id = Long.parseLong(mailid);
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            if (i == null) {
                return true;
            }
            return i.complete();
        } finally {
            sessionManager.close();
        }
    }

    @Override
    public synchronized QueueInfo next() {
        log.info("next: ");
        if (currentQueue != null && currentQueue.isEmpty()) {
            currentQueue = null;
        }
        if (currentQueue == null) {
            Session session = sessionManager.open();
            try {
                Date now = currentDateService.getNow();
                List<EmailItem> items = EmailItem.findToSend(now, session);
                if (items == null || items.isEmpty()) {
                    log.info("Nothing to send");
                    return null;
                }
                currentQueue = new ArrayList<>();
                for (EmailItem i : items) {
                    QueueInfo info = new QueueInfo(aspirinConfiguration, listenerManager);
                    info.setMailid(i.getId() + "");
                    info.setRecipient(i.getRecipientAddress());
                    currentQueue.add(info);
                }
            } finally {
                sessionManager.close();
            }
        }

        QueueInfo next = currentQueue.remove(0);

        // Mark it as pending
        Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();
        try {
            Date now = currentDateService.getNow();
            Long id = Long.parseLong(next.getMailid());
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            i.setSendStatus("p");
            i.setSendStatusDate(now);
            session.save(i);
            tx.commit();
        } finally {
            sessionManager.close();
        }
        return next;
    }

    @Override
    public void remove(String mailid) {
        log.info("remove: " + mailid); // ignored
    }

    @Override
    public void removeRecipient(String recipient) {
        log.info("removeRecipient: " + recipient); // ignored
    }

    @Override
    public void setSendingResult(QueueInfo qi) {
        log.info("setSendingResult: " + qi.getMailid() + " - " + qi.getResultInfo() + " - " + qi.getAttempt());
        Session session = sessionManager.open();
        try {
            Long id = Long.parseLong(qi.getMailid());
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            if (i == null) {
                return;
            }

            EmailSendAttempt a = new EmailSendAttempt();
            a.setEmailItem(i);
            a.setStatus(qi.getResultInfo());
            a.setStatusDate(currentDateService.getNow());
            session.save(a);

            i.setSendStatusDate(currentDateService.getNow());
            if (qi.getState() == DeliveryState.FAILED) {
                i.setSendStatus("f");
            } else if (qi.getState() == DeliveryState.SENT) {
                i.setSendStatus("c");
            } else if (qi.getState() == DeliveryState.IN_PROGRESS) {
                i.setSendStatus("p");                
            } else if (qi.getState() == DeliveryState.QUEUED) {
                long tm = currentDateService.getNow().getTime();
                tm = tm + retryIntervalMs;
                Date retryDate = new Date(tm);
                int attempts = 0;
                if (i.getNumAttempts() != null) {
                    attempts = i.getNumAttempts();
                }
                attempts++;
                i.setNextAttempt(retryDate); // when next to attempt delivery
                i.setNumAttempts(attempts);
                i.setSendStatus("r"); // is now a retry

            }
            session.save(i);

        } finally {
            sessionManager.close();
        }
        // TODO: increment counter or something???
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }
}
