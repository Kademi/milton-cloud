package io.milton.cloud.process;

import java.io.Serializable;

/**
 *
 * @author brad
 */
public interface Rule extends Serializable {
    
    
    /**
     * Evaluate this rule on the given context
     * 
     * @param context
     * @return - true to indicate this rule is satisfied
     */
    boolean eval(ProcessContext context);

}
