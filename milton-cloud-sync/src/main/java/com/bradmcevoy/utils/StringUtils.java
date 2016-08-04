package com.bradmcevoy.utils;

import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.ArrayList;

public class StringUtils {
    
    private Random random;
    
    public StringUtils() {
    }
    
    public static java.sql.Date now() {
        return new java.sql.Date( new java.util.Date().getTime() );
    }
    
    public static java.sql.Date addYear( java.sql.Date dt ) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( new java.util.Date(dt.getTime()) );
        cal.add(Calendar.YEAR,1);
        return new java.sql.Date( cal.getTimeInMillis() );
    }
    
    /** Returns the date in the form dd/MM/yyyy */
    public static synchronized String getTextFromDate( java.util.Date date ) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }
    

    /** Returns the date in the form dd/MM/yyyy hh:mm */
    public static synchronized String getTextFromDateTime( java.util.Date date ) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    /** Parses the text to date using the format dd/MM/yyyy */
    public static synchronized java.util.Date getDateFromText( String dt ) throws ParseException{
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
        return sdf.parse(dt);
    }
    
    
    /** Parses the text to date using the format dd/MM/yyyy */
    public static synchronized java.util.Date getDateAndTimeFromText( String dt ) throws ParseException{
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm");
        return sdf.parse(dt);
    }
    
    public static synchronized java.util.Date getDateFromText( String day, String month, String year ) throws ParseException{
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        String s = day + "/" + month + "/" + year;
        return sdf.parse(s);
    }
    
    public static synchronized java.util.Date getDateAndTimeFromText( String day, String month, String year, String hour, String minute ) throws ParseException{
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm");
        String s = day + "/" + month + "/" + year + " " + hour + ":" + minute;
        return sdf.parse(s);
    }
    
    public static synchronized java.util.Date getDateAndTimeFromText( int day, int month, int year, int hour, int minute ) throws ParseException{
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm");
        String s = day + "/" + month + "/" + year + " " + hour + ":" + minute;
        return sdf.parse(s);
    }
    
    /**
     * Adds a specified number of months to current calendar date and
     * return a SQL date.
     * @param Calendar
     * @param no of months to be added
     * @returns java.sql.Date
     */
    public synchronized static java.sql.Date addMonth(java.util.Calendar cal, int noOfMonths){
        cal.add(java.util.Calendar.MONTH, noOfMonths);
        return getSQLDate(cal.getTime());
    }
    
    /**
     * Adds a week to current calendar date and
     * return a SQL date.
     * @param Calendar
     * @returns java.sql.Date one week ahead
     */
    public synchronized static java.sql.Date addWeek(java.util.Calendar cal){
        cal.add(java.util.Calendar.DAY_OF_WEEK, 7);
        return getSQLDate(cal.getTime());
    }
    /**
     * Coverts  a java.util.Date to a java.sql.Date
     *@param java.util.Date type
     *@returns java.sql.Date type
     */
    public synchronized static java.sql.Date getSQLDate(java.util.Date date) {
        if( date == null ) return null;
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());        
        return sqlDate;
    }
    
    public synchronized static java.sql.Date getSQLDate(String strDate) throws java.text.ParseException {
        return getSQLDate( getDateFromText(strDate) );   
    }

    /**
     * Converts a java.sql.Date to a java.util.Date
     *@param java.sql.Date type
     *@returns java.util.Date type
     */
    public synchronized static java.util.Date getDateFromSQLDate(java.sql.Date date) {
        if( date == null ) return null;
        java.util.Date dt = new java.util.Date(date.getTime());        
        return dt;
    }
    
    
    /**
     * Gets the local system date
     */
    public synchronized static java.sql.Date getTodaysDate() {
        java.sql.Date date = new java.sql.Date(new java.util.Date().getTime());
        return date ;
    }
    
    public synchronized static java.sql.Timestamp getSystemClock(){
        java.sql.Timestamp newTime = new java.sql.Timestamp(new java.util.Date().getTime());
        return newTime;
    }
    
    public synchronized static Vector<String> split(String source, String token) {
        Vector<String> v = new Vector<String>();
        _split(source,v, token);
        return v;
    }
    
    private synchronized static void _split(String source, Vector<String> v, String param) {
        int pos = source.indexOf(param);
        String id;
        
        if( pos >= 0 ) {
            id = source.substring(0,pos).trim() ;
            if( id.length() > 0 ) 	v.add( id );
            _split(source.substring(pos + param.length() ,source.length()),v,param);
        } else {
            id = source.trim() ;
            if( id.length() > 0 )   v.add( id );
        }
    }
    
    public static String[] getResource(String name) throws Exception{
        Vector<String> v = new Vector<String>();
        URL url = StringUtils.class.getClassLoader().getResource(name);
        InputStream str = url.openStream();
        StringBuffer sb = new StringBuffer();
        int i = str.read();
        while( i > -1 ) {
            if( i == 10 ) {
                v.add( sb.toString() );
                sb = new StringBuffer();
            } else if( i != 13 ) {
                sb.append((char)i);
            }
            i = str.read();
        }
        
        i = 0;
        String[] settings = new String[v.size()];
        Iterator it = v.iterator();
        while( it.hasNext() ) {
            settings[i] = (String)it.next();
            i++;
        }
        
        return settings;
    }
    
    public static String getResourceAsString(String name) throws Exception{
        Vector v = new Vector();
        URL url = StringUtils.class.getClassLoader().getResource(name);
        InputStream str = url.openStream();
        StringBuffer sb = new StringBuffer();
        int i = str.read();
        while( i > -1 ) {
            sb.append((char)i);
            i = str.read();
        }
        return sb.toString();
    }
    
    public synchronized static String[][] getResource(String name,int cols,String delimiter) throws Exception{
        int row = 0;
        int c = 0;
        String phase = "get resource";
        try{
            Vector<String> v = new Vector<String>();
            URL url = v.getClass().getClassLoader().getResource(name);
            
            phase = "open resource";
            InputStream str = url.openStream();
            
            phase = "read resource";
            StringBuffer sb = new StringBuffer();
            int i = str.read();
            while( i > -1 ) {
                if( i == 10 ) {
                    v.add( sb.toString() );
                    sb = new StringBuffer();
                } else if( i != 13 ) {
                    sb.append((char)i);
                }
                i = str.read();
            }
            
            phase = "copy resource to array";
            String[][] vals = new String[v.size()][cols];
            Iterator it = v.iterator();
            while( it.hasNext() ) {
                String s1 = (String)it.next();
                Vector v2 = split(s1, delimiter);
                Iterator it2 = v2.iterator();
                c = 0;
                while( it2.hasNext() ) {
                    String s2 = (String)it2.next();
                    vals[row][c] = s2;
                }
            }
            
            return vals;
        } catch(Exception e) {
            throw new Exception("There was an error reading resource " + name + " in phase " + phase + ". Row=" + row + "; column=" + c);
        }
    }
    
    public synchronized static String replace(String source, String param, String value) {
        String s = "";
        Vector<String> v = new Vector<String>();
        _replace(source, v, param,value);
        Iterator it = v.iterator();
        while( it.hasNext() ) {
            s += (String)it.next();
        }
        return s;
    }
    
    private synchronized static void  _replace(String source, Vector<String> v,String param, String value) {
        int pos = source.indexOf(param);
        String id;
        
        if( pos >= 0 ) {
            id = source.substring(0,pos); //.trim() ;
            if( id.length() > 0 ) 	v.add( id );
            v.add( value );
            
            _replace(source.substring(pos + param.length() ,source.length()),v,param,value);
        } else {
            //	    id = source.trim() ;
            id = source;
            if( id.length() > 0 ) v.add( id );
        }
    }
    
    public String systemFileSeperator() {
        return System.getProperty("file.separator");
    }

    public String randomMessage(String[] strings) {
        if( random == null ) random = new Random();
        int i = random.nextInt(strings.length-1);
        return strings[i];
    }

    public boolean isNumeric(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public boolean in(String string, String[] list) {
        for( String s : list ) {
            if( string.equals(s) ) return true;
        }
        return false;
    }
    
    public static String toString(List<String> list, char delimiter) {
        if( list == null || list.size()==0 ) return "";
        StringBuffer sb = null;
        for( String s : list ) {
            if( sb == null ) sb = new StringBuffer();
            else sb.append(delimiter);
            sb.append(s);
        }
        return sb.toString();
    }
    
    public static String toString(List<String> list ) {
        return toString(list,',');
    }
 
    public static List<String> fromString(String s) {
        return fromString(s,',');
    }
    
    public static List<String> fromString(String s, char delimiter) {
        List<String> list = new ArrayList<String>();
        if( s == null || s.length() == 0 ) return list;
        for( String part : s.split("[" + delimiter + "]")) {
            list.add(part);
        }
        return list;
    }
}
