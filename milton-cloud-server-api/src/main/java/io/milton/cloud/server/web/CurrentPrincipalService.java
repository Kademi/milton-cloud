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
 * Just an abstraction on how to associated the current operation with a principal, ie the user
 * account the current operation is operating within the context of
 * 
 * Usually this will be delegated to a property of the Http request, but for batch
 * processing it will use a thread local
 *
 * @author brad
 */
public interface CurrentPrincipalService {
    UserResource getCurrentPrincipal();
    void setCurrentPrincipal(UserResource user);
}
