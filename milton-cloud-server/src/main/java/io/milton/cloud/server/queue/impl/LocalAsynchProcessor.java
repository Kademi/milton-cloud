package io.milton.cloud.server.queue.impl;

import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.queue.Processable;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import org.hibernate.Session;

public class LocalAsynchProcessor implements AsynchProcessor {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LocalAsynchProcessor.class);
    private final java.util.concurrent.LinkedBlockingQueue<Processable> queue = new LinkedBlockingQueue<>();
    private final RootContext rootContext;
    private final SessionManager sessionManager;
    private Thread threadProcessor;
    private final List<Processable> scheduledJobs = new ArrayList<>();
    private Timer scheduler;
    private boolean running;

    public LocalAsynchProcessor(RootContext rootContext, SessionManager sessionManager) {
        if (rootContext == null) {
            throw new NullPointerException("No rootContext was provided");
        }
        this.rootContext = rootContext;
        this.sessionManager = sessionManager;
    }

    @Override
    public void enqueue(Processable p) {
        log.info("Enqueued: " + p.getClass());
        this.queue.add(p);
    }

    @Override
    public void schedule(final Processable p, long period) {        
        this.scheduledJobs.add(p);
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                log.info("running scheduled task: " + p);
                runProcessable(p);
            }
        };
        log.info("scheduling job: " + p + " with period: " + period);
        scheduler.scheduleAtFixedRate(task, 1000, period);
    }

    @Override
    public void start() {
        log.debug("AysnchProcessor starting...");
        log.debug("..starting queue processor");
        rootContext.put(this);
        threadProcessor = new Thread(new QueueProcessor());
        threadProcessor.setDaemon(true);
        this.running = true;
        threadProcessor.start();

        log.debug("..starting scheduler");
        scheduler = new Timer(this.getClass().getCanonicalName(), true);

        log.debug("AysnchProcessor started");
    }

    @Override
    public void stop() {
        log.debug("*** LocalAsynchProcessor shutdown ***");
        running = false;
        if( threadProcessor != null ) {
            threadProcessor.interrupt();
            threadProcessor = null;
        }        
        if( scheduler != null ) {
            scheduler.cancel();
            scheduler = null;
        }
    }


    public class QueueProcessor implements Runnable {
        @Override
        public void run() {
            while ( queue != null && running) {
                try {
                    final Processable a = queue.take();
                    runProcessable(a);
                } catch (InterruptedException ex) {
                    log.warn("QueueProcessor terminated by interrupt command", ex);
                    running = false;
                } catch (Throwable e) {
                    log.error("Exception processing: ", e);
                }
            }
            log.warn("QueueProcessor stopped: running: " + running + " queue null?" + (queue==null));
        }
    }

    void runProcessable(final Processable p) {
        log.info("runProcessable: " + p);
        rootContext.execute(new Executable2() {

            @Override
            public void execute(Context context) {
                log.info("execute: " + p.getClass());
                Session session = sessionManager.open();
                context.put(session);
                p.doProcess(context);
                sessionManager.close();
            }
        });

    }
}  