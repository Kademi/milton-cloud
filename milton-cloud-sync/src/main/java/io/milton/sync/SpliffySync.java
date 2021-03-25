package io.milton.sync;

import io.milton.common.Path;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.sync.triplets.JdbcLocalTripletStore;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import io.milton.sync.event.FileChangedEvent;
import io.milton.sync.triplets.HttpTripletStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SpliffySync {

    private static final Logger log = LoggerFactory.getLogger(SpliffySync.class);

    private final File localRoot;
    private final DbInitialiser dbInit;
    private final Host httpClient;
    private final Syncer syncer;
    private final String basePath;
    private final Archiver archiver;
    private final HttpTripletStore remoteTripletStore;
    private final JdbcLocalTripletStore jdbcTripletStore;
    private final JdbcSyncStatusStore statusStore;
    private final SyncingDeltaListener deltaListener2;
    private final EventManager eventManager;
    private ScheduledExecutorService scheduledExecService;
    private ScheduledFuture<?> scanJob;
    private boolean paused;

    private boolean jobScheduled;
    private DirWalker dirWalker;

    public SpliffySync(File local, Host httpClient, String basePath, Syncer syncer, Archiver archiver, DbInitialiser dbInit, EventManager eventManager, boolean localReadonly) throws IOException {
        this.localRoot = local;
        this.httpClient = httpClient;
        this.basePath = basePath;
        this.syncer = syncer;
        this.archiver = archiver;
        this.dbInit = dbInit;
        this.eventManager = eventManager;
        remoteTripletStore = new HttpTripletStore(httpClient, Path.path(basePath));
        jdbcTripletStore = new JdbcLocalTripletStore(dbInit.getUseConnection(), dbInit.getDialect(), localRoot, eventManager);
        statusStore = new JdbcSyncStatusStore(dbInit.getUseConnection(), dbInit.getDialect(), basePath, localRoot);
        deltaListener2 = new SyncingDeltaListener(syncer, archiver, localRoot, statusStore, jdbcTripletStore);
        deltaListener2.setReadonlyLocal(localReadonly);
//        try {
//            // Now subscribe to server push notifications
//            pushClient = new TcpChannelClient(httpClient.server, 7020);
//            pushClient.registerListener(this);
//        } catch (UnknownHostException ex) {
//            log.error("exception setting up push notifications", ex);
//        }

    }

    /**
     * Perform an immediate scan
     *
     * @throws io.milton.httpclient.HttpException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     * @throws ConflictException
     * @throws NotFoundException
     * @throws IOException
     */
    public void walk() throws HttpException, NotAuthorizedException, BadRequestException, ConflictException, NotFoundException, IOException {
//        jdbcTripletStore.scan();
        dirWalker = new DirWalker(remoteTripletStore, jdbcTripletStore, statusStore, deltaListener2);

        // Now do the
        dirWalker.walk();
    }

    /**
     * Start the syncronisation service. This will schedule the first scan after
     * a short delay, then will scan at intervals after that.
     *
     */
    public void start() {
        // subscribe to receive notifications when the file system changes
        eventManager.registerEventListener(new SpliffySyncEventListener(), FileChangedEvent.class);
        scheduledExecService = Executors.newScheduledThreadPool(1);
        // schedule a job to do the scanning with a fixed interval
        scanJob = scheduledExecService.scheduleWithFixedDelay(new ScanRunner(),5000, 60000*10, TimeUnit.MILLISECONDS); // scan at 10 min intervals
        jdbcTripletStore.start();
//        pushClient.start();

//        log.info("Authenticate to push manager");
//        AuthenticateMessage msg = new AuthenticateMessage();
//        msg.setUsername(httpClient.user);
//        msg.setPassword(httpClient.password);
//        msg.setWebsite(httpClient.server);
//        pushClient.sendNotification(msg);
    }

    public void stop() {
        if (scanJob != null) {
            scanJob.cancel(true);
            scanJob = null;
        }
        if (scheduledExecService != null) {
            scheduledExecService.shutdownNow();
            scheduledExecService = null;
        }
        jdbcTripletStore.stop();
    }

    public void setPaused(boolean state) {
        if( dirWalker != null ) {
            dirWalker.setCanceled(paused);
        }
        paused = state;
        syncer.setPaused(state);
    }

    public boolean isPaused() {
        return paused;
    }

    public Archiver getArchiver() {
        return archiver;
    }

    public DbInitialiser getDbInit() {
        return dbInit;
    }

    public String getBasePath() {
        return basePath;
    }

    public SyncingDeltaListener getDeltaListener2() {
        return deltaListener2;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Host getHttpClient() {
        return httpClient;
    }

    public JdbcLocalTripletStore getJdbcTripletStore() {
        return jdbcTripletStore;
    }

    public File getLocalRoot() {
        return localRoot;
    }

    public HttpTripletStore getRemoteTripletStore() {
        return remoteTripletStore;
    }

    public ScheduledFuture<?> getScanJob() {
        return scanJob;
    }

    public ScheduledExecutorService getScheduledExecService() {
        return scheduledExecService;
    }

    public JdbcSyncStatusStore getStatusStore() {
        return statusStore;
    }

    public Syncer getSyncer() {
        return syncer;
    }

//    @Override
//    public void handleNotification(UUID sourceId, Serializable msg) {
//        if( jobScheduled ) {
//            log.info("handleNotification: already scheduled");
//            return ;
//        } else {
//            log.info("handleNotification: schedule a scan");
//        }
//        jobScheduled = true; // try to ensure that if we get many rapid notifications we dont
//        scheduledExecService.schedule(new ScanRunner(), 500, TimeUnit.MILLISECONDS); // give a little delay
//    }
//
//    @Override
//    public void memberRemoved(UUID sourceId) {
//
//    }
//
//    @Override
//    public void onConnect() {
//
//    }



    private class ScanRunner implements Runnable {

        @Override
        public void run() {
            jobScheduled = false;
            try {
                if( !paused ) {
                    log.info("ScanRunner: doing scan of: " + localRoot.getAbsolutePath());
                    walk();
                } else {
                    log.info("ScanRunner: is paused: " + localRoot.getAbsolutePath());
                }
            } catch (HttpException | NotAuthorizedException | BadRequestException | ConflictException | NotFoundException | IOException ex) {
                log.error("Exception during scan", ex);
            } catch(Throwable e) {
                log.error("Exception during scan", e);
            }

        }
    }

    private class SpliffySyncEventListener implements EventListener {

        @Override
        public void onEvent(Event e) {
            if (e instanceof FileChangedEvent) {
                log.info("File changed event, doing scan..");
                if( !paused ) {
                    scheduledExecService.submit(new ScanRunner());
                }
            }
        }
    }
}
