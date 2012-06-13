/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.contacts;

import java.io.IOException;
import io.milton.cloud.server.db.utils.SessionManager;
import io.milton.ldap.LdapTransactionManager;

/**
 *
 * @author brad
 */
public class SpliffyLdapTransactionManager implements LdapTransactionManager{

    private final SessionManager sessionManager;

    public SpliffyLdapTransactionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    
    @Override
    public void tx(Runnable r) throws IOException {
        sessionManager.open();
        try {
            r.run();
        } finally {
            sessionManager.close();
        }
    }
    
}
