package io.milton.cloud.process;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author brad
 */
public class StateProcessBuilder {

    private final StateProcess process;
    private Map<String, StateBuilder> mapOfStates = new HashMap<>();

    public StateProcessBuilder(String name, String startStateName) {
        process = new ProcessImpl(name);
        StateBuilder sb = from(startStateName);
        process.setStartState(sb.state);
    }

    public StateProcess getProcess() {
        process.walkStates();
        return process;
    }

    public StateBuilder state(String name) {
        return mapOfStates.get(name);
    }

    public final StateBuilder from(String name) {
        StateBuilder sb = mapOfStates.get(name);
        if (sb == null) {
            State state = process.createState(name);
            sb = new StateBuilder(state);
            mapOfStates.put(name, sb);
        }
        return sb;
    }

    public class StateBuilder {

        private final State state;

        public StateBuilder(State state) {
            this.state = state;
        }

        public TransitionBuilder transition(String name) {
            TransitionBuilder tb = new TransitionBuilder(name);
            return tb;
        }

        public StateBuilder onEnter(ActionHandler h) {
            state.getOnEnterHandlers().add(h);
            return this;
        }

        public StateBuilder onExit(ActionHandler h) {
            state.getOnExitHandlers().add(h);
            return this;
        }

        public class TransitionBuilder {

            private String name;
            private Rule rule;
            private Transition transition;

            public TransitionBuilder(String name) {
                this.name = name;
            }

            public TransitionBuilder when(Rule r) {
                if (transition != null) {
                    transition.setRule(rule);
                }
                this.rule = r;
                return this;
            }

            public TransitionBuilder to(String toStateName) {
                StateBuilder sb = from(toStateName);
                if (transition == null) {
                    transition = process.createTransition(name, state, sb.state, rule);
                } else {
                    transition.setToState(sb.state);
                }
                return this;
            }

            public TransitionBuilder to(StateBuilder toState) {
                if (transition == null) {
                    transition = process.createTransition(name, state, toState.state, rule);
                } else {
                    transition.setToState(toState.state);
                }
                return this;
            }

            public TransitionBuilder then(ActionHandler h) {
                transition.getOnTransitionHandlers().add(h);
                return this;
            }
        }
    }
}
