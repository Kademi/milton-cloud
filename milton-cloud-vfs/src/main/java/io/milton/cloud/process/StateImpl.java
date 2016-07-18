package io.milton.cloud.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StateImpl implements Serializable, State {

    private static final long serialVersionUID = 1L;
    private String name;
    private Map<String, StateProcess> subProcesses;
    private StateProcess process;
    private TimeDependentInterval interval;
    final Transitions transitions = new Transitions(this);
    final List<ActionHandler> onEnterHandlers = new ArrayList<>();
    final List<ActionHandler> onExitHandlers = new ArrayList<>();

    public StateImpl(StateProcess process, String name) {
        if (process == null) {
            throw new IllegalArgumentException("process cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("state name cannot be null");
        }
        if (!name.trim().equals(name)) {
            throw new IllegalArgumentException("state name cannot begin or end with white space");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("state name cannot be blank");
        }
        this.name = name;
        this.process = process;
    }

    @Override
    public Transitions getTransitions() {
        return transitions;
    }

    @Override
    public Map<String, StateProcess> getSubProcesses() {
        return subProcesses;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StateProcess getProcess() {
        return process;
    }

    @Override
    public List<ActionHandler> getOnEnterHandlers() {
        return onEnterHandlers;
    }

    @Override
    public List<ActionHandler> getOnExitHandlers() {
        return onExitHandlers;
    }

    @Override
    public TimeDependentInterval getInterval() {
        return interval;
    }


    @Override
    public void setInterval(TimeDependentInterval interval) {
        this.interval = interval;
    }

}
