package io.milton.cloud.process;

import io.milton.context.RootContext;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessImpl implements Serializable, StateProcess {

    private static final Logger log = LoggerFactory.getLogger(ProcessImpl.class);
    private static final long serialVersionUID = 1L;
    private State startState;
    final Map<String, State> statesMap = new HashMap<>();
    final private String name;

    public ProcessImpl(String name) {
        this.name = name;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public State createState(String stateName) {
        return new StateImpl(this, stateName);
    }

    public Collection<State> getStates() {
        return statesMap.values();
    }

    @Override
    public State getState(String stateName) {
        if (stateName == null) {
            throw new IllegalArgumentException("stateName is null");
        }
        return statesMap.get(stateName);
    }

    @Override
    public void setStartState(State start) {
        this.startState = start;
    }

    @Override
    public State getStartState() {
        return startState;
    }
 
    @Override
    public Transition createTransition(String transitionName, State fromState, State toState, Rule rule) {
        Transition t = new TransitionImpl(this, transitionName, fromState, toState, rule);
        return t;
    }

    @Override
    public void walkStates() {
        statesMap.clear();
        if( startState == null ) {
            throw new RuntimeException("Start state is not set");
        }
        State s = this.startState;
        walkStates(s);
    }

    private void walkStates(State s) {
        log.debug("walkState: " + s.getName());
        if (this.statesMap.containsValue(s)) {
            return;
        }
        if (this.statesMap.containsKey(s.getName())) {
            throw new RuntimeException("Duplicate state: " + s.getName());
        }

        this.statesMap.put(s.getName(), s);

        log.debug(".. transitions: " + s.getTransitions().size());
        for (Transition t : s.getTransitions()) {
            log.debug(".. ..transition: " + t.getName());
            walkStates(t.getToState());
        }
    }

    public void add(State s) {
        this.statesMap.put(s.getName(), s);
    }
}
