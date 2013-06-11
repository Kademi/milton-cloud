package io.milton.cloud.process;

import io.milton.cloud.common.CurrentDateService;
import io.milton.context.Contextual;
import io.milton.context.Registration;
import io.milton.context.RequestContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessContext implements Contextual {

    private static final Logger log = LoggerFactory.getLogger(ProcessContext.class);
    private final RequestContext parent;
    private final CurrentDateService currentDateService;
    private ProcessInstance token;
    private StateProcess process;
    private TimerService timerService;

    /**
     * Fill with use specific data. Eg customer object. Is NOT persisted
     */
    private Map<String, Object> attributes = new HashMap<>();
    public static final int MAX_TRANSITIONS = 10;

    public ProcessContext(RequestContext parent) {
        this.parent = parent;
        if (parent != null) {
            currentDateService = parent.get(CurrentDateService.class);
        } else {
            currentDateService = null;
        }
    }

    public ProcessContext(CurrentDateService currentDateService) {
        this.parent = null;
        this.currentDateService = currentDateService;
    }

    public ProcessContext() {
        this.parent = null;
        this.currentDateService = null;
    }

    public <T> Registration<T> put(T o) {
        return parent.put(o, null);
    }

    /**
     *
     * @param token
     * @param process
     * @param timerService - only needed if using time dependent states,
     * otherwise can be null
     */
    public ProcessContext(RequestContext requestContext, ProcessInstance token, StateProcess process, TimerService timerService, CurrentDateService currentDateService) {
        this.parent = requestContext;
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
        requestContext.put(token);
        requestContext.put(process);
        requestContext.put(timerService);
    }
//
//    public ProcessContext(CurrentDateService currentDateService) {
//        this.currentDateService = currentDateService;
//        this.token = null;
//        this.timerService = null;
//        this.process = null;
//    }

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
        put(val);
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
            state = getProcess().getStartState();
            getProcessInstance().setStateName(state.getName());
            if (log.isDebugEnabled()) {
                log.debug("start: current state is: " + getProcessInstance().getStateName());
            }
            didTransition = true;
        }
        for (Transition t : state.getTransitions()) {
            if (evalAndTransition(t, false)) {
                if (log.isDebugEnabled()) {
                    log.debug("transitioned to: " + getProcessInstance().getStateName());
                }
                return true;
            }
        }
        return didTransition;
    }

    public State getCurrentState() {
        if (getProcessInstance().getStateName() == null) {
            System.out.println("statename is null");
            return null;
        }
        State state = getProcess().getState(getProcessInstance().getStateName());
        if (state == null) {
            throw new RuntimeException("state not found: " + getProcessInstance().getStateName());
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
        if (log.isDebugEnabled()) {
            log.debug("executeTransition(" + getProcess().getName() + ") transitioning from " + transition.getFromState().getName() + " to " + transition.getToState().getName());
        }
        fireOnExit(transition.getFromState());
        transitionTo(transition);
        fireOnEnter(transition.getToState());
        if (transition.getToState().getInterval() != null) {
            if (getTimerService() != null) {
                getTimerService().registerTimer(this);
            }
        } else {
            if (getTimerService() != null) {
                getTimerService().unRegisterTimer(this);
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
        long tm = System.currentTimeMillis();
        State toState = transition.getToState();
        getProcessInstance().setStateName(toState.getName());
        getProcessInstance().setTimeEntered(currentDateService.getNow());
        for (ActionHandler handler : transition.getOnTransitionHandlers()) {
            handler.process(this);
            if (log.isDebugEnabled()) {
                log.debug("transitionTo: " + handler + " handler duration=" + (System.currentTimeMillis() - tm) + "ms");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("transitionTo: total duration=" + (System.currentTimeMillis() - tm) + "ms");
        }
    }

    public ProcessInstance getProcessInstance() {
        if (token == null) {
            token = parent.get(ProcessInstance.class);
        }
        return token;
    }

    public StateProcess getProcess() {
        if (process == null) {
            process = parent.get(StateProcess.class);
        }
        return process;
    }

    public TimerService getTimerService() {
        if (timerService == null) {
            timerService = parent.get(TimerService.class);
        }
        return timerService;
    }

    @Override
    public <T> T get(String id) {
        return parent.get(id);
    }

    @Override
    public <T> T get(Class<T> c) {
        return parent.get(c);
    }
}
