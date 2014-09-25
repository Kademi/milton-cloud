package com.ettrema.context;

/**
 *
 */
public class ClassBeanLocator implements BeanLocator{

    final Class classToFind;

    public ClassBeanLocator(Class classToFind) {
        this.classToFind = classToFind;
    }
    
    public Object locateBean(Context context) {
        return context.get(classToFind);
    }

}
