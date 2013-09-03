package io.milton.cloud.server.apps.scheduler;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.ScheduledEmail;
import io.milton.cloud.server.queue.Processable;
import io.milton.context.Context;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import java.io.IOException;

/**
 * Is periodically invoked by the AsyncProcessor to poll for learning-related
 * processes which need to be scanned
 *
 * @author brad
 */
public class SchedulerProcessor implements Processable, Serializable {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SchedulerProcessor.class);
    private final SchedulerApp schedulerApp;
    private final ApplicationManager applicationManager;

    public SchedulerProcessor(SchedulerApp schedulerApp, ApplicationManager applicationManager) {
        this.schedulerApp = schedulerApp;
        this.applicationManager = applicationManager;
    }

    @Override
    public void doProcess(Context context) {
        log.info("doProcess");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        Date now = _(CurrentDateService.class).getNow();
        ScheduledEmail.TakeResult takeResult = ScheduledEmail.takeDue(now, session);
        while (takeResult != null) {
            ScheduledEmail due = takeResult.getScheduledEmail();
            log.info("process due job: " + due.getSubject() + " ID=" + due.getId());
            try {
                Date fromDate = takeResult.getScheduledEmail().getStartDate();
                if (takeResult.getPreviousResult() != null) {
                    fromDate = takeResult.getPreviousResult().getStartDate();
                }
                schedulerApp.sendScheduledEmail(due, fromDate, takeResult.getThisResult().getStartDate(), session);
                session.save(due);
                session.flush();
                tx.commit();
                log.info("Completed sending job: " + due.getId());
            } catch (Exception ex) {
                log.error("exception", ex);
                tx.rollback();
            }
            tx = session.beginTransaction();
            takeResult = ScheduledEmail.takeDue(now, session);
        }

    }

    @Override
    public void pleaseImplementSerializable() {
    }
}
