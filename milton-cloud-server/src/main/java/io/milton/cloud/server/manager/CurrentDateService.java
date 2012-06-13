package io.milton.cloud.server.manager;

import java.util.Date;

/**
 * Simple interface to allow externalising the current date, ie for testing
 *
 * @author brad
 */
public interface CurrentDateService {
    Date getNow();
}
