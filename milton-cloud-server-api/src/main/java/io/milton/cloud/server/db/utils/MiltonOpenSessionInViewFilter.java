package io.milton.cloud.server.db.utils;

import io.milton.vfs.db.utils.SessionManager;
import io.milton.http.Filter;
import io.milton.http.FilterChain;
import io.milton.http.Request;
import io.milton.http.Response;




/**
 *
 * @author brad
 */
public class MiltonOpenSessionInViewFilter implements Filter {

    private final SessionManager sessionManager;

    public MiltonOpenSessionInViewFilter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void process(FilterChain chain, Request request, Response response) {
        try {
            sessionManager.open();
            chain.process(request, response);
        } finally {
            sessionManager.close();
        }

    }
}
