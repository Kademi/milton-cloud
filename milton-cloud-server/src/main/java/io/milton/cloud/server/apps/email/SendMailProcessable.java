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

import io.milton.cloud.server.queue.Processable;
import io.milton.context.Context;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;

import static io.milton.context.RequestContext._;

/**
 * TODO: this needs to be a static class, but currently needs a reference to
 * parent
 *
 */
public class SendMailProcessable implements Serializable, Processable {
    private static final long serialVersionUID = 1l;
    private long jobId;

    public SendMailProcessable(long jobId) {
        this.jobId = jobId;
    }

    @Override
    public void doProcess(Context context) {
        _(GroupEmailService.class).send(jobId, SessionManager.session());
    }

    @Override
    public void pleaseImplementSerializable() {
    }
    
}
