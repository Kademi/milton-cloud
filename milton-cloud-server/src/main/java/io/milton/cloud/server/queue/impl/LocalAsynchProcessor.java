package io.milton.cloud.server.queue.impl;

import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.queue.Processable;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.hibernate.Session;

public class LocalAsynchProcessor implements AsynchProcessor {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LocalAsynchProcessor.class);
    //private final java.util.concurrent.LinkedBlockingQueue<Processable> queue = new LinkedBlockingQueue<>();
    private final DelayQueue<DelayedQueueItem> queue = new DelayQueue<>();
    private final RootContext rootContext;
    private final SessionManager sessionManager;
    private Thread threadProcessor;
    private final List<Processable> scheduledJobs = new ArrayList<>();
    private Timer scheduler;
    private boolean running;
    private List<String> history = new ArrayList<>();

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
        this.queue.add(new DelayedQueueItem(p));
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
        scheduler.scheduleAtFixedRate(task, 60000*10, period); // first run is 10 mins after startup
    }

    @Override
    public void runScheduledJobs() {
        log.warn("runScheduledJobs");
        for (final Processable p : scheduledJobs) {
            log.info("run: " + p);

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    log.info("running manual task: " + p);
                    runProcessable(p);
                }
            };
            scheduler.schedule(task, 500);            
        
        }
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
        if (threadProcessor != null) {
            threadProcessor.interrupt();
            threadProcessor = null;
        }
        if (scheduler != null) {
            scheduler.cancel();
            scheduler = null;
        }
    }

    public class QueueProcessor implements Runnable {

        @Override
        public void run() {
            while (queue != null && running) {
                try {
                    final DelayedQueueItem a = queue.take();
                    runProcessable(a.processable);
                } catch (InterruptedException ex) {
                    log.warn("QueueProcessor terminated by interrupt command", ex);
                    running = false;
                } catch (Throwable e) {
                    log.error("Exception processing: ", e);
                }
            }
            log.warn("QueueProcessor stopped: running: " + running + " queue null?" + (queue == null));
        }
    }

    void runProcessable(final Processable p) {
        log.info("runProcessable: " + p);
        history.add(0, p.getClass().getCanonicalName() + " started at " + new Date());
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
        history.add(0, p.getClass().getCanonicalName() + " finished at " + new Date());

        while( history.size() > 100 ) {
            history.remove(history.size());
        }
        
    }

    public List<String> getHistory() {
        return history;
    }

    
    
    public class DelayedQueueItem implements Delayed {

        private final Processable processable;
        private final long origin;
        private final long delay;

        public DelayedQueueItem(final Processable processable) {
            this.origin = System.currentTimeMillis();
            this.processable = processable;
            this.delay = 5000;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            if (delayed == this) {
                return 0;
            }

            if (delayed instanceof DelayedQueueItem) {
                long diff = delay - ((DelayedQueueItem) delayed).delay;
                return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
            }

            long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
            return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
        }

        @Override
        public int hashCode() {
            final int prime = 31;

            int result = 1;
            result = prime * result + ((processable == null) ? 0 : processable.hashCode());

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (!(obj instanceof DelayedQueueItem)) {
                return false;
            }

            final DelayedQueueItem other = (DelayedQueueItem) obj;
            if (processable == null) {
                if (other.processable != null) {
                    return false;
                }
            } else if (processable != other.processable) {
                return false;
            }

            return true;
        }
    }
}
