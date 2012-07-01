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
package io.milton.cloud.server.mail;

import java.util.Collection;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.masukomi.aspirin.core.store.queue.QueueInfo;
import org.masukomi.aspirin.core.store.queue.QueueStore;

/**
 *
 * @author brad
 */
public class MiltonCloudQueueStore implements QueueStore{

    @Override
    public void add(String mailid, long expire, Collection<InternetAddress> recipients) throws MessagingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> clean() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueueInfo createQueueInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getNextAttempt(String mailid, String recipient) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasBeenRecipientHandled(String mailid, String recipient) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCompleted(String mailid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueueInfo next() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(String mailid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeRecipient(String recipient) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSendingResult(QueueInfo qi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
