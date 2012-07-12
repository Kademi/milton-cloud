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

import java.util.List;
import javax.mail.internet.MimeMessage;
import org.masukomi.aspirin.core.store.mail.MailStore;

/**
 *
 * @author brad
 */
public class EmailItemMailStore implements MailStore{

    @Override
    public MimeMessage get(String mailid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getMailIds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(String mailid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void set(String mailid, MimeMessage msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
