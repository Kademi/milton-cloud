package com.ettrema.logging;

import java.io.File;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author brad
 */
public class Log4JMonitor  implements Runnable  {

    private static Logger log = Logger.getLogger( Log4JMonitor.class );
    boolean interruped;
    private final long checkIntervalMillis;
    private File file;
    // stores the last modification time of the file
    private long lastModified = 0;

    public Log4JMonitor( long checkIntervalMillis, File file) {
        this.checkIntervalMillis = checkIntervalMillis;
        this.file = file;
    }



    public void run() {
        System.out.println( "Initialize " + file.getAbsolutePath() );
        lastModified = file.lastModified();
        monitor();
    }

    private void monitor() {
        log.info( "Starting log4j monitor" );

        while( !interruped ) {

            // check if File changed
            long temp = file.lastModified();
            if( lastModified != temp ) {
                log.info( "Initialize log4j configuration " + file.getAbsolutePath() );
                LogManager.resetConfiguration();
                PropertyConfigurator.configure( file.getAbsolutePath() );
                lastModified = temp;
            } else {
                if( log.isDebugEnabled() ) {
                    log.debug( "Log4j configuration is not modified: " + file.getAbsolutePath() + " - " + temp );
                }
            }
            try {
                Thread.sleep( checkIntervalMillis );
            } catch( InterruptedException e ) {
                interruped = true;
            }
        }
        log.info( "Shutting down log4j monitor" );
    }


    public long getCheckIntervalMillis() {
        return checkIntervalMillis;
    }

    public boolean isInterruped() {
        return interruped;
    }

    public void setInterruped( boolean interruped ) {
        this.interruped = interruped;
    }
}
