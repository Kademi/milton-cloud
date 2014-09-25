package com.bradmcevoy.utils;

import com.bradmcevoy.common.UnrecoverableException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);
    
    public static Object create(String className) {
        Class clazz;
        log.trace("create: " + className);
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new UnrecoverableException(className,ex);
        } catch (NoClassDefFoundError ex) {
            throw new UnrecoverableException(className,ex);
        } catch(UnsupportedClassVersionError ex) {
            throw new UnrecoverableException(className,ex);
        }
        Object o;
        try {
            o = clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new UnrecoverableException(className,ex);
        } catch (IllegalAccessException ex) {
            throw new UnrecoverableException(className,ex);
        }
        return o;
    }
    
    public static Object create(Class clazz) {
        Constructor cc;
        try {
            cc = clazz.getConstructor();
            return cc.newInstance();
        } catch (IllegalArgumentException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (SecurityException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (NoSuchMethodException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InstantiationException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (IllegalAccessException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InvocationTargetException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        }                       
    }
    
    public static Object create(Class clazz, Object arg1) {
        if( clazz == null ) throw new IllegalArgumentException("clazz is null");
        if( arg1 == null ) throw new IllegalArgumentException("arg1 is null");
        Constructor cc;
        try {
            Constructor best = null;
            for( Constructor con : clazz.getConstructors() ) {
                Class<?>[] types = con.getParameterTypes();
                if( types.length == 1 ) {
//                    if( best == null ) best = con;
                    //if( types[0].isAssignableFrom(arg1.getClass()) && types[0].isAssignableFrom(arg2.getClass()) ) {
                    boolean b1 = types[0].isAssignableFrom(arg1.getClass());
//                    log.debug("arg1: " + arg1.getClass().getName() + " == " + types[0].getName() + " -> " + b1);
//                    log.debug("arg2: " + arg2.getClass().getName() + " == " + types[1].getName() + " -> " + b2);
                    if( b1 ) {
                        best = con;
                    }
                }
            }
            if( best == null ) throw new NullPointerException("No suitable constructor found: " + clazz.getName());
            return best.newInstance(arg1);
        } catch (IllegalArgumentException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (SecurityException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InstantiationException ex) {
            System.out.println("ex: " + ex.getMessage());
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (IllegalAccessException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InvocationTargetException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        }               
    }
    
    public static Object create(Class clazz, Object arg1, Object arg2) {
        if( clazz == null ) throw new IllegalArgumentException("clazz is null");
        if( arg1 == null ) throw new IllegalArgumentException("arg1 is null");
        if( arg2 == null ) throw new IllegalArgumentException("arg2 is null");
        Constructor cc;
        try {
            Constructor best = null;
            for( Constructor con : clazz.getConstructors() ) {
                Class<?>[] types = con.getParameterTypes();
                if( types.length == 2 ) {
//                    if( best == null ) best = con;
                    //if( types[0].isAssignableFrom(arg1.getClass()) && types[0].isAssignableFrom(arg2.getClass()) ) {
                    boolean b1 = types[0].isAssignableFrom(arg1.getClass());
                    boolean b2 = types[1].isAssignableFrom(arg2.getClass()); 
//                    log.debug("arg1: " + arg1.getClass().getName() + " == " + types[0].getName() + " -> " + b1);
//                    log.debug("arg2: " + arg2.getClass().getName() + " == " + types[1].getName() + " -> " + b2);
                    if( b1 && b2 ) {
                        best = con;
                    }
                }
            }
            if( best == null ) throw new NullPointerException("No suitable constructor found: " + clazz.getName() + "(" + arg1.getClass().getName() + "," + arg2.getClass().getName() + ")");
            return best.newInstance(arg1,arg2);
        } catch (IllegalArgumentException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (SecurityException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InstantiationException ex) {
            System.out.println("ex: " + ex.getMessage());
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (IllegalAccessException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InvocationTargetException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        }               
    }

    public static Object create(Class clazz, Object arg1, Object arg2, Object arg3) {
        Constructor cc;
        try {
            Constructor best = null;
            for( Constructor con : clazz.getConstructors() ) {
                Class<?>[] types = con.getParameterTypes();
                if( types.length == 3 ) {
//                    if( best == null ) best = con;
                    //if( types[0].isAssignableFrom(arg1.getClass()) && types[0].isAssignableFrom(arg2.getClass()) ) {
                    boolean b1 = types[0].isAssignableFrom(arg1.getClass());
                    boolean b2 = types[1].isAssignableFrom(arg2.getClass()); 
                    boolean b3 = types[2].isAssignableFrom(arg3.getClass()); 
//                    log.debug("arg1: " + arg1.getClass().getName() + " == " + types[0].getName() + " -> " + b1);
//                    log.debug("arg2: " + arg2.getClass().getName() + " == " + types[1].getName() + " -> " + b2);
                    if( b1 && b2 && b3 ) {
                        best = con;
                    }
                }
            }
            if( best == null ) throw new NullPointerException("No suitable constructor found: " + clazz.getName() );
            return best.newInstance(arg1,arg2,arg3);
        } catch (IllegalArgumentException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (SecurityException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InstantiationException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (IllegalAccessException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        } catch (InvocationTargetException ex) {
            throw new UnrecoverableException(clazz.getName(),ex);
        }               
    }
    
    public static Object create(String className, Object arg1) {
        Class c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(className,ex);
        }
        return create(c,arg1);        
    }
    
    public static Object create(String className, Object arg1, Object arg2) {
        Class c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(className,ex);
        }
        return create(c,arg1,arg2);
    }
    
    public static Object create(String className, Object arg1, Object arg2, Object arg3) {
        Class c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(className,ex);
        }
        return create(c,arg1,arg2,arg3);
    }

    public static Class findClass(String sClass) {
        try {
            Class c = Class.forName(sClass);
            return c;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to find class: " + sClass, ex);
        }
    }
    
}
