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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.TriggerTimer;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.MenuItem;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class ViewTriggerTimersPage extends TemplatedHtmlPage {

    public ViewTriggerTimersPage(String name, CommonCollectionResource parent) {
        super(name, parent, "email/viewTriggerTimers", "View Trigger Timers");
        MenuItem.setActiveIds("menuTalk", "menuEmails", "menuAutoEmail");
    }

    @Override
    protected Map<String, Object> buildModel(Map<String, String> params) {
        Map<String, Object> map = super.buildModel(params);
        Date now = _(CurrentDateService.class).getNow();
        Date weekAgo = _(Formatter.class).addDays(now, -14);
        Session session = SessionManager.session();
        List<TriggerTimer> list = TriggerTimer.search(getOrganisation(), weekAgo, null, session); 
        map.put("recentTriggerTimers", list);
        return map;
    }
    
    public String status(TriggerTimer tt) {
        if( tt.getCompletedProcessingAt() != null ) {
            return "Complete";
        } else {
            if( tt.getNumAttempts() == 0 ) {
                return "Waiting..";
            } else {
                return "Processing, attempt=" + tt.getNumAttempts();
            }
        }
    }
    
}
