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
import io.milton.cloud.server.db.AbstractEmailJobVisitor;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.db.EmailSendAttempt;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.vfs.db.utils.SessionManager;
import java.util.*;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.hibernate.Cache;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.masukomi.aspirin.core.config.Configuration;
import org.masukomi.aspirin.core.listener.ListenerManager;
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
    private long retryIntervalMs = 30 * 1000l; // 30secs
    private int maxAttempts = 3;

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
        return Collections.EMPTY_LIST;
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
        Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();
        try {
            log.info("init: reset any in progress statuses");
            List<EmailItem> items = EmailItem.findInProgress(session);
            log.info("EmailItems to reset inprogress state to retry: " + items.size());
            for (EmailItem i : items) {
                i.setSendStatus("r");
                session.save(i);
            }

            log.info("init: check for any completed group email jobs");
            for (GroupEmailJob j : GroupEmailJob.findInProgress(session)) {
                Date now = currentDateService.getNow();
                log.info("Check: " + j.getTitle());
                j.checkStatus(now, session);
            }
            tx.commit();
        } finally {
            sessionManager.close();
        }
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
        if (currentQueue != null && currentQueue.isEmpty()) {
            currentQueue = null;
        }
        if (currentQueue == null) {
            Session session = sessionManager.open();
            try {
                Date now = currentDateService.getNow();
                List<EmailItem> items = EmailItem.findToSend(now, session);
                if (items == null || items.isEmpty()) {
                    // No remaining items, so check for any completed jobs to tidy up
                    List<GroupEmailJob> inProgress = GroupEmailJob.findInProgress(session);
                    if (!inProgress.isEmpty()) {
                        Transaction tx = session.beginTransaction();
                        for (GroupEmailJob j : inProgress) {
                            j.checkStatus(now, session);
                        }
                        tx.commit();
                    }

                    // Convenient place to show memory usage
                    long free = Runtime.getRuntime().freeMemory();
                    long total = Runtime.getRuntime().totalMemory();
                    long max = Runtime.getRuntime().maxMemory();
                    long used = total - free;
                    long actualPerc = used * 100 / max;
                    long maxMb = max / (1024 * 1024);
//                    Cache c = sessionManager.getCache();

                    //System.out.println("cache: " + c.getClass());
                    log.info("next: Nothing more to send. Memory used=" + actualPerc + "%" + " of " + maxMb + "Mb");
                    return null;
                }
                log.info("next: Loaded queue size: " + items.size());
                currentQueue = new ArrayList<>();
                for (EmailItem i : items) {
                    if (canSend(i)) {
                        QueueInfo info = new QueueInfo(aspirinConfiguration, listenerManager);
                        info.setMailid(i.getId() + "");
                        info.setRecipient(i.getRecipientAddress());
                        currentQueue.add(info);
                    } else {
                        log.info("Not adding emailitem because job is disabled. EmailItemId=" + i.getId());
                    }
                }
            } finally {
                sessionManager.close();
            }
        }

        QueueInfo next = null;
        while (next == null && !currentQueue.isEmpty()) {
            next = currentQueue.remove(0);
            log.info("Current queue size: " + currentQueue.size());

            // Mark it as pending
            Session session = sessionManager.open();
            Transaction tx = session.beginTransaction();
            try {
                Date now = currentDateService.getNow();
                Long id = Long.parseLong(next.getMailid());
                EmailItem i = (EmailItem) session.get(EmailItem.class, id);
                if (canSend(i)) {
                    session.refresh(i);
                    if (i.isReadyToSend()) {
                        i.setSendStatus(EmailItem.STATUS_PENDING);
                        i.setSendStatusDate(now);
                        session.save(i);
                        session.flush();
                        tx.commit();
                        log.info("next item: " + i.getRecipientAddress() + " attempts: " + i.getNumAttempts() + " id: " + i.getId());
                    } else {
                        log.info("EmailItem is no longer in the ready status, so ignore: " + i.getId() + " - status=" + i.getSendStatus());
                    }

                } else {
                    log.info("Not taking EmailItem because job is not active: " + i.getId());
                    next = null;
                }
            } finally {
                sessionManager.close();
            }
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
        final Session session = sessionManager.open();
        Transaction tx = session.beginTransaction();
        try {
            Long id = Long.parseLong(qi.getMailid());
            EmailItem i = (EmailItem) session.get(EmailItem.class, id);
            if (i == null) {
                return;
            }
            log.info("setSendingResult: " + i.getRecipientAddress() + " - attempt=" + i.getNumAttempts() + " emailId=" + i.getId());
            log.info("   resultinfo: " + qi.getResultInfo() + " state=" + qi.getState());

            EmailSendAttempt a = new EmailSendAttempt();
            a.setEmailItem(i);
            a.setStatus(qi.getResultInfo());
            a.setStatusDate(currentDateService.getNow());
            session.save(a);

            i.setSendStatusDate(currentDateService.getNow());
            //boolean sentOk = qi.getResultInfo() != null && qi.getResultInfo().startsWith("250");
            // BM: fastmail is rejecting emails with resultInfo=451 4.7.1 <admin@bradmcevoy.com>: Recipient address rejected: Temporary deferral, try again soon
            // and delivery state is QUEUED
            // BUT, i think i've seen the queued state before where delivery was queued by the remote server, so we should not send again
            // If this is true then we should check the code in resultInfo to decide what to do, 451 definitely means retry

            // 250 2.0.0 Ok: queued as 69277F8010A
            // 451 4.7.1 <admin@bradmcevoy.com>: Recipient address rejected: Temporary deferral, try again soon
            String sStatus = qi.getResultInfo();
            Integer status = SmtpUtils.getStatusCode(sStatus);
            if (status != null && (status >= 200 && status < 300)) {
                log.info("setting complete email status complete: status code=" + status + " sStatus=" + sStatus);
                i.setSendStatus(EmailItem.STATUS_COMPLETE);
            } else {
                long tm = currentDateService.getNow().getTime();
                tm = tm + retryIntervalMs;
                Date retryDate = new Date(tm);
                int attempts = 0;
                if (i.getNumAttempts() != null) {
                    attempts = i.getNumAttempts();
                }
                if (attempts >= maxAttempts) {
                    i.setSendStatus(EmailItem.STATUS_FAILED);
                    log.warn("Set retry. Reached max attempts=" + attempts + " for id: " + i.getId());
                } else {
                    attempts++;
                    i.setNextAttempt(retryDate); // when next to attempt delivery
                    i.setNumAttempts(attempts);
                    i.setSendStatus(EmailItem.STATUS_RETRY); // is now a retry
                    log.warn("Set retry. Attempts=" + attempts + " for id: " + i.getId());
                }
            }

            session.save(i);
            // Check if this is the last email in a batch job, in which case we mark it as completed
            log.info("check if completed");
            BaseEmailJob job = i.getJob();
            if (job != null) {
                log.info("we have a job");
                job.accept(new AbstractEmailJobVisitor() {
                    @Override
                    public void visit(GroupEmailJob r) {
                        Date now = currentDateService.getNow();
                        r.checkStatus(now, session);
                    }
                });
            } else {
                log.info("email item not attached to a job");
            }

            tx.commit();
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

    private boolean canSend(EmailItem i) {
        return i.getJob() == null || i.getJob().isActive() || i.forceSend();
    }
}
