package io.milton.cloud.process;

import io.milton.cloud.common.CurrentDateService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessContext {

    private static final Logger log = LoggerFactory.getLogger(ProcessContext.class);
    private final ProcessInstance token;
    private final StateProcess process;
    private final TimerService timerService;
    private final CurrentDateService currentDateService;
    /**
     * Fill with use specific data. Eg customer object. Is NOT persisted
     */
    private Map<String, Object> attributes = new HashMap<>();
    public static final int MAX_TRANSITIONS = 10;

    /**
     *
     * @param token
     * @param process
     * @param timerService - only needed if using time dependent states,
     * otherwise can be null
     */
    public ProcessContext(ProcessInstance token, StateProcess process, TimerService timerService, CurrentDateService currentDateService) {
        this.token = token;
        this.process = process;
        this.timerService = timerService;
        this.currentDateService = currentDateService;
        if (process == null) {
            throw new NullPointerException("process is null");
        }
        if (token == null) {
            throw new NullPointerException("token is null");
        }
    }

    /**
     * Scan for triggers on the current state. This is recursive, so if a
     * transition occurs it will scan again, and so forth. Because of the
     * likelihood of infinite recursion, this implements a maximum number of
     * auto transitions - see MAX_TRANSITIONS
     */
    public boolean scan() {
        log.debug("scan");
        int cnt = 0;
        boolean didTransition = false;
        while (_scan()) {
            didTransition = true;
            if (cnt++ > MAX_TRANSITIONS) {
                throw new RuntimeException("Exceeded maximum auto transitions: " + cnt + ". Check for infinite recursion in process");
            }
        }
        return didTransition;
    }

    public void addAttribute(String key, Object val) {
        attributes.put(key, val);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    private boolean _scan() {
        boolean didTransition = false;
        State state = getCurrentState();
        if (state == null) {
            state = process.getStartState();
            token.setStateName(state.getName());
            log.info("start: current state is: " + token.getStateName());
            didTransition = true;
        }
        for (Transition t : state.getTransitions()) {
            if (evalAndTransition(t, false)) {
                log.info("transitioned to: " + token.getStateName());
                return true;
            }
        }
        return didTransition;
    }

    public State getCurrentState() {
        if (token.getStateName() == null) {
            System.out.println("statename is null");
            return null;
        }
        State state = process.getState(token.getStateName());
        if (state == null) {
            throw new RuntimeException("state not found: " + token.getStateName());
        }
        return state;
    }

    public boolean fireTransition(String transitionName) {
        State state = getCurrentState();
        if (state == null) {
            throw new IllegalArgumentException("No current state");
        }
        Transition t = state.getTransitions().get(transitionName);
        if (t == null) {
            throw new IllegalArgumentException("No transition: " + transitionName + " from state: " + state.getName());
        }
        return evalAndTransition(t, true);
    }

    /**
     * If this transition's rule is true, execute the transition and return true
     *
     * @param context
     * @return - true iff the transition executed
     */
    private boolean evalAndTransition(Transition transition, boolean fireOnNoRule) {
        if (transition.getRule() == null) {
            if (fireOnNoRule) {
                executeTransition(transition);
                return true;
            } else {
                return false;
            }
        } else if (transition.getRule().eval(this)) {
            executeTransition(transition);
            return true;
        } else {
            return false;
        }
    }

    void executeTransition(Transition transition) {
        if (log.isInfoEnabled()) {
            log.info("executeTransition(" + process.getName() + ") transitioning from " + transition.getFromState().getName() + " to " + transition.getToState().getName());
        }
        fireOnExit(transition.getFromState());
        transitionTo(transition);
        fireOnEnter(transition.getToState());
        if (transition.getToState().getInterval() != null) {
            if (timerService != null) {
                timerService.registerTimer(this);
            }
        } else {
            if (timerService != null) {
                timerService.unRegisterTimer(this);
            }
        }

        // now scan again in case the next transition is already valid
        scan();
    }

    void fireOnEnter(State state) {
        // allow all transitions to arm themselves. eg start timers
        for (Transition t : state.getTransitions()) {
            Rule r = t.getRule();
            if (r instanceof TimeDependentRule) {
                ((TimeDependentRule) r).arm(this);
            }
        }
        for (ActionHandler handler : state.getOnEnterHandlers()) {
            handler.process(this);
        }
    }

    void fireOnExit(State state) {

        // allow all timers to disarm themselves, eg stop timers
        for (Transition t : state.getTransitions()) {
            Rule r = t.getRule();
            if (r instanceof TimeDependentRule) {
                ((TimeDependentRule) r).disarm(this);
            }
        }
        for (ActionHandler handler : state.getOnExitHandlers()) {
            handler.process(this);
        }
    }

    /**
     * Update internal state and forward to token
     *
     * @param toState
     */
    void transitionTo(Transition transition) {
        State toState = transition.getToState();
        token.setStateName(toState.getName());
        token.setTimeEntered(currentDateService.getNow());
        for (ActionHandler handler : transition.getOnTransitionHandlers()) {
            handler.process(this);
        }
    }

    public ProcessInstance getProcessInstance() {
        return token;
    }

    public StateProcess getProcess() {
        return process;
    }
}
