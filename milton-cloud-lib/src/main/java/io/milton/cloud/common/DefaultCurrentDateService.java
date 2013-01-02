package io.milton.cloud.common;

import java.util.Date;

/**
 * Just returns a new Date to give the actual system time
 *
 * @author brad
 */
public class DefaultCurrentDateService implements CurrentDateService {

    public DefaultCurrentDateService() {
    }

    
    
    @Override
    public Date getNow() {
        return new Date();
    }
}
