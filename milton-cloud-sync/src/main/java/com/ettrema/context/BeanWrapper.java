package com.ettrema.context;

/**
 *
 * @author brad
 */
public class BeanWrapper implements Factory {

    private Object bean;

    public Class[] keyClasses() {
        return null;
    }

    public String[] keyIds() {
        return null;
    }

    public Registration insert(RootContext context, Context requestContext) {
        throw new UnsupportedOperationException( "should already be in context");
    }

    public void init(RootContext context) {
        context.put( bean);
    }

    public void destroy() {

    }

    public void onRemove(Object item) {

    }
}
