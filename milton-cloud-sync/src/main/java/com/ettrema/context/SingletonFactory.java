package com.ettrema.context;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author brad
 */
public class SingletonFactory implements Factory{

    private static final Logger log = LoggerFactory.getLogger(SingletonFactory.class);

    private Object bean;
    private Class[] keyClasses;

    @Override
    public Class[] keyClasses() {
        return keyClasses;
    }

    @Override
    public String[] keyIds() {
        return null;
    }

    @Override
    public Registration insert(RootContext context, Context requestContext) {
        return context.put(bean);
    }

    @Override
    public void init(RootContext context) {

    }

    @Override
    public void destroy() {
        if( bean instanceof Closeable) {
            log.warn( "closing: " + bean.getClass());
            Closeable c = (Closeable) bean;
            try {
                c.close();
            } catch( IOException ex ) {
                log.error("Exception closing: " + bean.getClass(), ex);
            }
        } else {
            log.warn( "not closing: " + bean.getClass());
        }
    }

    @Override
    public void onRemove(Object item) {

    }

    public String getBeanClass() {
        return bean.getClass().getCanonicalName();
    }

    public String getKeyClasses() {
        String s = "";
        for( Class c : keyClasses ) {
            s = s + c.getCanonicalName() + ",";
        }
        return s;
    }

    public void setBean( Object bean ) {
        this.bean = bean;
        List<Class> list = new ArrayList<Class>();
        list.add( bean.getClass());
        log.debug("setBean: " + bean.getClass().getCanonicalName());
        for(Class c :  this.bean.getClass().getInterfaces() ) {
            list.add( c );
            log.debug(" - add interface: " + c.getCanonicalName());
        }
        Class superClass = this.bean.getClass().getSuperclass();
        while( superClass != null ) {
            list.add(superClass);
            superClass = superClass.getSuperclass();
        }

        this.keyClasses = new Class[list.size()];
        list.toArray( keyClasses );
    }

    public Object getBean() {
        return bean;
    }

    @Override
    public String toString() {
        if( bean != null ) {
            return "SingletonFactory(" + bean.getClass() + ")";
        } else {
            return super.toString();
        }
    }




}
