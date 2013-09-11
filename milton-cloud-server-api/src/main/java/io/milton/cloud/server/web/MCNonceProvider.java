/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import io.milton.cloud.common.With;
import io.milton.cloud.server.db.LoginNonce;
import io.milton.http.Request;
import io.milton.http.http11.auth.NonceProvider;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class MCNonceProvider implements NonceProvider {

    private final SessionManager sessionManager;
    private long validitySecs = 60 * 60 * 24; // by default nonces are valid for 1 day
    private long expiredSecs = 60 * 60 * 24 * 7; // by default, nonces are reported as expired for 7 days. After that they are reported not found

    public MCNonceProvider(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public NonceValidity getNonceValidity(String nonce, Long nonceCount) {
        LoginNonce n = LoginNonce.get(nonce, SessionManager.session());
        if (n == null) {
            return NonceValidity.INVALID;
        } else {
            return getNonceValidity(n.getCreatedAt());
        }
    }

    @Override
    public String createNonce(Request request) {

        LoginNonce n = sessionManager.doLocalTx(new With<Session, LoginNonce>() {
            @Override
            public LoginNonce use(Session session) throws Exception {
                Transaction tx = session.beginTransaction();
                LoginNonce n = new LoginNonce();
                UUID id = UUID.randomUUID();
                Date now = new Date();

                n.setNonce(id.toString());
                n.setCreatedAt(now);
                session.save(n);
                tx.commit();
                return n;
            }
        });
        return n.getNonce();
    }

    private NonceValidity getNonceValidity(Date issued) {
        long dif = (System.currentTimeMillis() - issued.getTime()) / 1000;
        if( dif < validitySecs ) {
            return NonceValidity.OK;
        } else {
            if( dif < expiredSecs ) {
                return NonceValidity.EXPIRED;
            } else {
                return NonceValidity.INVALID;
            }
        }
    }
}
