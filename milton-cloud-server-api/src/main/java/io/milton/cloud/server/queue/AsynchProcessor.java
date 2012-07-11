package io.milton.cloud.server.queue;

import com.ettrema.common.Service;

/**
 * Simple interface for enqueueing objects for ayncronous processing
 *
 * @author brad
 */
public interface AsynchProcessor extends Service {

    void enqueue(Processable p);

}
