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

import io.milton.mail.MailResourceFactory;
import io.milton.mail.Mailbox;
import io.milton.mail.MailboxAddress;

/**
 *
 * @author brad
 */
public class MiltonCloudMailResourceFactory implements MailResourceFactory{

    @Override
    public Mailbox getMailbox(MailboxAddress add) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
