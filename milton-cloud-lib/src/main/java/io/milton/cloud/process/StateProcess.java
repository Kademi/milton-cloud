package io.milton.cloud.process;


/**
 *
 * @author brad
 */
public interface StateProcess {
        
    /**
     * The name of this process
     * 
     * @return 
     */
    String getName();

    /**
     * Create a new state, initially disconnected from the graph
     * 
     * @param stateName
     * @return 
     */
    State createState(String stateName);

    Transition createTransition(String transitionName, State from, State to, Rule rule);

    /**
     * Locate the named state
     */
    State getState(String stateName);
    
    /**
     * Return the initial state for new process instances
     * 
     * @return 
     */
    State getStartState();

    /**
     * Set the start state for new process instances
     * 
     * @param start 
     */
    void setStartState(State start);

    
    /**
     * Must call this after adding states programmatically. Automatically called
     * when loading xml
     */
    void walkStates();

}
