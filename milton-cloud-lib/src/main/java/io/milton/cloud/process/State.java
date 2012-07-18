package io.milton.cloud.process;

import java.util.List;
import java.util.Map;

/**
 * A state is a definition for a state that a process instance may be in.
 *
 * Note that a state exists only within a process definition.
 *
 * A state may be time dependent. This means that the rules within the state
 * need to be evaluated periodically to check for transitions. This time dependence
 * is represented by the presence of a time dependent interval value, which will
 * specify the interval that the state should be polled for changed.
 *
 * @author brad
 */
public interface State {

    public enum TimeDependentInterval {
        NONE,
        MINUTE,
        HOUR,
        DAY,
        WEEK
    }

    String getName();

    List<ActionHandler> getOnEnterHandlers();

    List<ActionHandler> getOnExitHandlers();

    StateProcess getProcess();

    Map<String, StateProcess> getSubProcesses();

    Transitions getTransitions();

    TimeDependentInterval getInterval();

    void setInterval(TimeDependentInterval interval);


}
