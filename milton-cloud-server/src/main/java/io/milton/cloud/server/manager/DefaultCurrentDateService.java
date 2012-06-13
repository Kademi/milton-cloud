package io.milton.cloud.server.manager;

import java.util.Date;

/**
 * Just returns a new Date to give the actual system time
 *
 * @author brad
 */
public class DefaultCurrentDateService implements CurrentDateService {

	@Override
    public Date getNow() {
        return new Date();
    }

}
