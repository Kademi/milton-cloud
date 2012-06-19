package io.milton.cloud.server.apps.calendar;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author brad
 */
public class CalendarUtils {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( CalendarUtils.class );

    public static String formatDate( Date date ) {
        log.debug("formatDate: " + date);
        if( date == null ) {
            return "";
        }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime( date );
        return formatDate( cal );
    }

    public static String formatDate( java.util.Calendar cal ) {
        if( cal == null ) {
            return "";
        }
        int offsetHours = cal.getTimeZone().getOffset( cal.getTimeInMillis())/(1000*60*60);
        int rawHour = cal.get( Calendar.HOUR_OF_DAY );
        int hour = rawHour + offsetHours;
        log.debug( "offsets: offset: " + offsetHours + " raw:" + rawHour + " final: " + hour);

        StringBuilder sb = new StringBuilder();
        sb.append( cal.get( Calendar.YEAR ) + "" );
        sb.append( '-' );
        sb.append( pad2( cal.get( Calendar.MONTH ) + 1 ) );
        sb.append( '-' );
        sb.append( pad2( cal.get( Calendar.DAY_OF_MONTH ) ) );
        sb.append( 'T' );
        sb.append( pad2( rawHour ) );
        sb.append( ':' );
        sb.append( pad2( cal.get( Calendar.MINUTE ) ) );
        sb.append( ':' );
        sb.append( pad2( cal.get( Calendar.SECOND ) ) );

        String s = sb.toString();
//        log.debug(date.toString() + " -> " + s);
        return s;
    }

    public static String pad2( int i ) {
        if( i < 10 ) {
            return "0" + i;
        } else {
            return i + "";
        }
    }

    /**
     * Just change to a normal java.util.Date if it is a java.sql.Timestamp
     * @param dt
     * @return 
     */
    static Date plainDate(Date dt) {
        if( dt instanceof Timestamp) {
            return new Date(dt.getTime());
        } else {
            return dt;
        }
    }
}
