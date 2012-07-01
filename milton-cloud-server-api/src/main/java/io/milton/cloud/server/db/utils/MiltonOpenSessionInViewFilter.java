package io.milton.cloud.server.db.utils;

import io.milton.cloud.server.manager.MCRootContext;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.vfs.db.utils.SessionManager;
import io.milton.http.Filter;
import io.milton.http.FilterChain;
import io.milton.http.Request;
import io.milton.http.Response;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class MiltonOpenSessionInViewFilter implements Filter {

    private final SessionManager sessionManager;
    private final MCRootContext rootContext;

    public MiltonOpenSessionInViewFilter(SessionManager sessionManager, MCRootContext rootContext) {
        this.sessionManager = sessionManager;
        this.rootContext = rootContext;
    }

    @Override
    public void process(final FilterChain chain, final Request request, final Response response) {
        rootContext.execute(new Executable2() {

            @Override
            public void execute(Context context) {
                try {
                    Session s = sessionManager.open();
                    context.put(s);
                    chain.process(request, response);
                } finally {
                    sessionManager.close();
                }
            }
        });
    }
}
