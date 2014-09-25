package com.ettrema.context;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ListFactory implements Factory<List>{

    final String[] ids;
    final List<BeanLocator> beanLocators;

    public ListFactory(String id, List<BeanLocator> beanLocators) {
        ids = new String[] {id};
        this.beanLocators = beanLocators;
    }

    public Class[] keyClasses() {
        return null; // only key by id
    }

    public String[] keyIds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Registration<List> insert(RootContext context, Context requestContext) {
        List list = new ArrayList();
        for( BeanLocator bl : beanLocators ) {
            Object o = bl.locateBean(requestContext);
            list.add(o);
        }
        Registration<List> reg = new Registration<List>(list, null, context);
        return reg;
    }

    public void init(RootContext context) {
        
    }

    public void destroy() {
        
    }

    public void onRemove(List item) {
        
    }

}
