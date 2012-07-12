/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.email;

import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.mail.Event;
import io.milton.mail.Filter;
import io.milton.mail.FilterChain;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class MCMailFilter implements Filter {

    private final SessionManager sessionManager;
    private final RootContext rootContext;

    public MCMailFilter(SessionManager sessionManager, RootContext rootContext) {
        this.sessionManager = sessionManager;
        this.rootContext = rootContext;
    }

    @Override
    public void doEvent(final FilterChain chain, final Event event) {

        rootContext.execute(new Executable2() {

            @Override
            public void execute(Context context) {                
                try {
                    Session session = sessionManager.open();
                    context.put(session);
                    chain.doEvent(event);
                } finally {
                    sessionManager.close();
                }

            }
        });
    }
}
