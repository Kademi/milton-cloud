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

/**
 * Used when there is no Http request, such as in batch processing
 *
 * @author brad
 */
public class ThreadLocalCurrentPrincipalService implements CurrentPrincipalService{

    private final ThreadLocal<UserResource> thUser = new ThreadLocal<>();
    
    @Override
    public UserResource getCurrentPrincipal() {
        return thUser.get();
    }

    @Override
    public void setCurrentPrincipal(UserResource user) {
        thUser.set(user);
    }
    
}
