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

import io.milton.http.Auth;
import io.milton.http.HttpManager;

/**
 * If there is a HTTP request associated with the thread then use it, otherwise
 * delegate current user guff to a wrapped CurrentPrincipalService, if present
 *
 * @author brad
 */
public class HttpManagerCurrentPrincipalService implements CurrentPrincipalService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HttpManagerCurrentPrincipalService.class);
    private final CurrentPrincipalService wrapped;

    public HttpManagerCurrentPrincipalService(CurrentPrincipalService wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public UserResource getCurrentPrincipal() {
        if (HttpManager.request() == null) {
            if (wrapped != null) {
                return wrapped.getCurrentPrincipal();
            } else {
                return null;
            }
        }
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null || auth.getTag() == null) {
            //log.warn("no auth object");
            return null;
        }
        if (auth.getTag() instanceof UserResource) {
            UserResource ur = (UserResource) auth.getTag();
            if (ur == null) {
                log.warn("Got auth object but null tag");
            }

            return ur;
        }
        return null;
    }

    @Override
    public void setCurrentPrincipal(UserResource p) {
        if( HttpManager.request() == null ) {
            if( wrapped != null ) {
                wrapped.setCurrentPrincipal(p);
            }
            return ;
        }
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null) {
            auth = new Auth(p.getName(), p);
            HttpManager.request().setAuthorization(auth);
        } else {
            auth.setTag(p);
        }
    }
}
