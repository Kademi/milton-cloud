package io.milton.cloud.process;

/**
 * Instances of this are fired on various events, such as entering or leaving states
 *
 * Must have a contructor (Element el)
 * 
 * @author brad
 */
public interface ActionHandler {
    
    /**
     * Perform the action
     * 
     * @param context
     */
    void process(ProcessContext context);
}
