package io.milton.cloud.process;

import java.util.List;


/**
 *
 * @author brad
 */
public interface Transition {

    void setFromState(State fromState);

    State getFromState();

    String getName();

    Rule getRule();

    State getToState();
    
    void setToState(State to);

    void setRule(Rule rule);
    
    List<ActionHandler> getOnTransitionHandlers();

}
