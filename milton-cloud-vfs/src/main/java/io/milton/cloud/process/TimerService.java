package io.milton.cloud.process;

/**
 * Used to record time dependent states of process instances
 *
 * Note that implementations must correctly check for previous state when
 * registering and unregistering. Eg if registering, its possible that the
 * process has already been registered in which case it should do nothing. And
 * same for unregistering - ie there might not be anything to unregister
 *
 * @author brad
 */
public interface TimerService {
    /**
     * Ensure the given process instance is registered for timer scanning. Note that the instance
     * may already be registered
     *
     * @param context
     */
    public void registerTimer(ProcessContext context);

    /**
     * Ensure that the process instance is not registered for timer scanning. It
     * might already not be registered
     *
     * @param context
     */
    public void unRegisterTimer(ProcessContext context);
}
