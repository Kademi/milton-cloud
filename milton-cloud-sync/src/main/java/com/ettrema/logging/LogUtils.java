package com.ettrema.logging;

import org.apache.log4j.Logger;

/**
 *
 * @author bradm
 */
public class LogUtils {
    public static void trace(Logger log, Object ... args) {
        if( log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for(Object o : args) {
                sb.append(o).append(", ");
            }
            log.trace(sb);
        }
    }
	
    public static void info(Logger log, Object ... args) {
        if( log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            for(Object o : args) {
                sb.append(o).append(", ");
            }
            log.info(sb);
        }
    }	
}
