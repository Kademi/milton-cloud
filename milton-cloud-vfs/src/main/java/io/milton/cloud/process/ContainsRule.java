/*
 */
package io.milton.cloud.process;

import io.milton.context.Registration;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Returns true if the given List contains at least 1 item which has a property
 * that matches the given value
 *
 * @author brad
 */
public class ContainsRule implements Rule {

    private String propertyName;
    private String value;

    public ContainsRule(String propertyName, String value) {
        this.propertyName = propertyName;
        this.value = value;
    }
        

    @Override
    public Boolean eval(ProcessContext context) {
        try {
            List arg = context.get(List.class);
            if (arg == null) {
                return false;
            }
            for (Object o : arg) {
                String propVal = BeanUtils.getProperty(o, propertyName);
                if( propVal != null && propVal.equals(value)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
