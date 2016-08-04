package com.bradmcevoy.utils;

public class IntegerUtils {
    
    public IntegerUtils() {
    }
    
    /** Returns true if the specified value exists in the array
     */
    public static boolean isInArray( int[] array, int valueToCheck) {
        for( int i=0; i<array.length; i++ ) {
            if( array[i] == valueToCheck ) {
                return true;
            }
        }
        return false;
    }
    
    
    public static Integer parseInteger(String s) {
        if( s == null ) return null;
        s = s.trim();
        if( s == null || s.length() == 0  ) return null;
        return Integer.parseInt(s);
    }    
}
