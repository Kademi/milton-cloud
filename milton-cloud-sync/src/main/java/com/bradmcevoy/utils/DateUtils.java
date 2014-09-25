package com.bradmcevoy.utils;

import java.util.*;
import java.text.*;

/** This class supercedes the functions in StringUtils
 *
 *  Wraps formatting information for convenience. An instance created with the
 *  default constructor has methods which will use the default format. This
 *  instance has methods which return instances created for each other known
 *  format.
 *  Eg
 *  DateUtils du = new DateUtils();
 *
 *  // default
 *  System.out.println( du.getText("31/1/2004") );
 *
 *  // us format, date only
 *  System.out.println( du.us().date().getText("31/1/2004") );
 *
 *  // australian format, date and time
 *  System.out.println( du.aus().dateTime().getText("31/1/2004") );
 */
public class DateUtils {

    private Region mUs;
    private Region mAus;
    private ThreadLocal<SimpleDateFormat> thSdf;
    public static String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    public static String[] dayAbbrevs = {"Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"};
    public final static String DEFAULT_FORMAT = "dd/MM/yyyy HH:mm";

    public DateUtils() {
        thSdf = new ThreadLocal<SimpleDateFormat>() {

            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat sdf = new SimpleDateFormat( DEFAULT_FORMAT );
                sdf.setLenient( false );
                return sdf;
            }
        };
    }

    public DateUtils( final String dateFormat, final boolean includeTime ) {
        thSdf = new ThreadLocal<SimpleDateFormat>() {

            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat sdf;
                if( includeTime ) {
                    sdf = new SimpleDateFormat( dateFormat + " HH:mm" );
                } else {
                    sdf = new SimpleDateFormat( dateFormat );
                }

                sdf.setLenient( false );
                return sdf;
            }
        };
    }

    public Region us() {
        if( mUs == null ) mUs = new Region( "MM/dd/yyyy" );
        return mUs;
    }

    public Region aus() {
        if( mAus == null ) mAus = new Region( "dd/MM/yyyy" );
        return mAus;
    }

    public String getText( java.util.Date date ) {
        if( date == null ) {
            return "";
        } else {
            return thSdf.get().format( date );
        }
    }

    public java.util.Date getDate( String dt ) throws ParseException {
        if( dt == null || dt.length() == 0 ) {
            return null;
        } else {
            return thSdf.get().parse( dt );
        }
    }

    public static java.sql.Date getSQLDate( java.util.Date dt ) {
        if( dt == null ) {
            return null;
        } else {
            return new java.sql.Date( dt.getTime() );
        }
    }

    public java.sql.Date getSQLDate( String strDate ) throws java.text.ParseException {
        Date dt = getDate( strDate );
        return getSQLDate( dt );
    }

    public static java.sql.Date addDays( java.sql.Date dt, int num ) {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.DAY_OF_MONTH, num );
        return getSQLDate( cal.getTime() );
    }

    public static Date addDays( Date dt, int num ) {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.DAY_OF_MONTH, num );
        return cal.getTime();
    }


    public String getDayName( int i ) {
        return dayNames[i];
    }

    public class Region {

        private DateUtils mDate;
        private DateUtils mDateTime;
        private String mDateFormat;

        public Region( String dateFormat ) {
            mDateFormat = dateFormat;
        }

        public DateUtils date() {
            if( mDate == null ) mDate = new DateUtils( mDateFormat, false );
            return mDate;
        }

        public DateUtils dateTime() {
            if( mDateTime == null )
                mDateTime = new DateUtils( mDateFormat, true );
            return mDateTime;
        }
    }
}
