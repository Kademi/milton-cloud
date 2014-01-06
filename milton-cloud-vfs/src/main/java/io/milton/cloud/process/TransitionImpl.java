
package io.milton.cloud.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A connection from one state to another. Defines criteria for how the transition
 *  can occur
 * 
 * @author brad
 */
public class TransitionImpl implements Serializable, Transition{
    
    private static final long serialVersionUID = 1L;
    
    private StateProcess process;
    private String name;
    private State fromState;
    private State toState;
    private Rule rule;
    
    private List<ActionHandler> onTransitionHandlers = new ArrayList<>();
    
    public TransitionImpl(StateProcess process, String name) {
        this.process = process;
        this.name = name;
    }
    
    public TransitionImpl(StateProcess process, String name, State fromState, State toState, Rule rule) {
        this(process,name);
        this.fromState = fromState;
        this.toState = toState;
        this.rule = rule;
    }

        

    @Override
    public State getToState() {
        return toState;
    }

    @Override
    public void setToState(State toState) {
        this.toState = toState;
    }

    
    @Override
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State getFromState() {
        return fromState;
    }

    @Override
    public void setFromState(State fromState) {
        this.fromState = fromState;
    }

    @Override
    public List<ActionHandler> getOnTransitionHandlers() {
        return onTransitionHandlers;
    }
    
    
}
