package io.milton.cloud.process;

import java.util.ArrayList;

public class Transitions extends ArrayList<Transition> {
    
    private static final long serialVersionUID = 3707385917870101891L;
    
    private final State fromState;
    
    public Transitions(State state) {
        super();
        this.fromState = state;
    }

    @Override
    public boolean add(Transition t) {
        if( get(t.getName()) != null ) throw new IllegalArgumentException("State " + fromState.getName() + " already contains transition: " + t.getName());
        return super.add(t);
    }    
    
   
    public Transition get(String name) {
        for( Transition t : this ) {
            if( t.getName().equals(name)) return t;
        }
        return null;
    }
}
