package io.milton.cloud.process;

/**
 * Extension to Rule which has hooks for activating and de-activating timers
 *
 * @author brad
 */
public interface TimeDependentRule extends Rule {
    /**
     * Called when a token enters the state which contains this rule
     * 
     * @param context
     */
    void arm(ProcessContext context);
    
    /*  Called when a token leaves the state which contains this rule
     */
    void disarm(ProcessContext context);    
}
