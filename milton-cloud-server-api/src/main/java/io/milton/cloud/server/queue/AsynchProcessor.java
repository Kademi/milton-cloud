package io.milton.cloud.server.queue;

import com.ettrema.common.Service;
import java.util.List;

/**
 * Simple interface for enqueueing objects for ayncronous processing
 *
 * @author brad
 */
public interface AsynchProcessor extends Service {

    void enqueue(Processable p);

    void schedule(final Processable p, long periodMillis);
    
    /**
     * manually initiate execution of scheduled jobs
     * 
     */
    void runScheduledJobs();
    
    /**
     * Get a log of recent events
     * 
     * @return 
     */
    List<String> getHistory();
}
