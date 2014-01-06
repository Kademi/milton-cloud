/*
 */
package io.milton.cloud.process;

import io.milton.context.Registration;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Filter out elements of a list which do not match the bean property expression
 * 
 * Can take a child expression, in which case it is passed the filtered list 
 * and returns the child's result
 *
 * @author brad
 */
public class ListFilterExpression implements Expression<Object> {

    private String propertyName;
    private String value;
    private Expression child;

    public ListFilterExpression(String propertyName, String value, Expression child) {
        this.propertyName = propertyName;
        this.value = value;
        this.child = child;
    }
        

    @Override
    public Object eval(ProcessContext context) {
        try {
            List arg = context.get(List.class);
            if (arg == null) {
                return arg;
            }
            List newList = new ArrayList();
            for (Object o : arg) {
                String propVal = BeanUtils.getProperty(o, propertyName);
                if( propVal != null && propVal.equals(value)) {
                    newList.add(o);
                }
            }
            if( child == null ) {
                return newList;
            } else {
                Registration<List> reg = context.put(newList);
                try {
                    return child.eval(context);
                } finally {
                    reg.remove();
                }                
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
