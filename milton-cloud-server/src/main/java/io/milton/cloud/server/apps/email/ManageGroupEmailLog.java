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
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


/**
 *
 * @author brad
 */
public class ManageGroupEmailLog extends TemplatedHtmlPage {

    private final GroupEmailJob job;
    
    public ManageGroupEmailLog(String name, CommonCollectionResource parent, GroupEmailJob job) {
        super(name, parent, "email/manageEmailJobLog", "Email Log");
        this.job = job;
    }

    @Override
    protected Map<String, Object> buildModel(Map<String, String> params) {
        Map<String, Object> map = super.buildModel(params);
        map.put("job", job);
        map.put("items", job.listItems(Boolean.TRUE, SessionManager.session()));
        return map;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuTalk", "menuEmails", "menuSendEmail");
        super.sendContent(out, range, params, contentType);
    }

    

    
}
