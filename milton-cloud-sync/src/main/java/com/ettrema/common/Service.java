package com.ettrema.common;

/**
 * Represents some service which can be controlled (ie started and stopped)
 *
 * @author brad
 */
public interface Service {
    /**
     * Start the service. Until this is called the service should not be functional
     */
    void start();

    /**
     * The service should be made unavailable, however it should be able to
     * be starte again
     *
     */
    void stop();
}
