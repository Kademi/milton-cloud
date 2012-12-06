/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server;

import io.milton.cloud.common.CurrentDateService;
import io.milton.context.RequestContext;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;



/**
 *
 * @author brad
 */
public class DataSessionManager {
    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final CurrentDateService currentDateService;

    public DataSessionManager(BlobStore blobStore, HashStore hashStore, CurrentDateService currentDateService) {
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.currentDateService = currentDateService;
    }

    public DataSession get(Long id) {
        Branch b = Branch.get(id, SessionManager.session());
        return get(b);
    }
    
    public DataSession get(Branch b) {
        String key = "branch" + b.getId();
        RequestContext ctx = RequestContext.getCurrent();
        DataSession dataSession = ctx.get(key);
        if( dataSession == null ) {
            Session session = SessionManager.session();
            dataSession = new DataSession(b, session, hashStore, blobStore, currentDateService);
            ctx.put(key, dataSession);
        }
        return dataSession;
    }
    
    
}
