package com.ettrema.logging;

import com.ettrema.common.Service;
import java.io.File;

/**
 *
 * @author brad
 */
public class Log4JMonitorService implements Service {

    private Log4JMonitor logMonitor;
    private Thread thread;
    private final File logFile;
    private final long interval;

    public Log4JMonitorService( File logFile, long interval ) {
        this.logFile = logFile;
        this.interval = interval;
        if( !logFile.exists() ) {
            throw new RuntimeException( "Log file does not exist: " + logFile.getAbsolutePath());
        }
    }

    public void start() {
        logMonitor = new Log4JMonitor( interval, logFile );
        thread = new Thread( logMonitor );
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }
}
