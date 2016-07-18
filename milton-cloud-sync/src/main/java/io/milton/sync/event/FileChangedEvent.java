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
package io.milton.sync.event;

import io.milton.event.Event;


/**
 * Fired when one or more changed files are detected in the file system.
 * 
 * This will usually inform the sync program after local triplet store has been updated.
 * The sync client will then scan the local triplet store against the remote.
 *
 * @author brad
 */
public class FileChangedEvent implements Event{
    
}
