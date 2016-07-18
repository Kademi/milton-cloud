package com.ettrema.context;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContext;

/**
 *
 */
public class SpringFactory implements Factory {
    private Class[] classes;
    private String id;

    public Class[] keyClasses() {
        return classes;
    }

    public String[] keyIds() {
        return new String[] {id};
    }

    public Registration insert(RootContext context, Context requestContext) {
        ApplicationContext appContext = requestContext.get(ApplicationContext.class);
        Object o = appContext.getBean(id);
        return context.put(o);
    }

    public void init(RootContext context) {
    }

    public void destroy() {
    }

    public void onRemove(Object item) {
    }


    /**
     * @return the classes
     */
    public String getClasses() {
        String s = "";
        for(Class clazz : classes ) {
            s = s +"," + clazz.getCanonicalName();
        }
        return s;
    }

    /**
     * @param classes the classes to set
     */
    public void setClasses(String classes) {
        String[] classNames = classes.split(",");
        List<Class> list = new ArrayList<Class>();
        for( String className : classNames ) {
            Class clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(className, ex);
            }
            list.add(clazz);
        }
        this.classes = new Class[list.size()];
        list.toArray(this.classes);
    }

    /**
     * @return the ids
     */
    public String getId() {
        return id;
    }

    /**
     * @param ids the ids to set
     */
    public void setIds(String id) {
        this.id = id;
    }


}
