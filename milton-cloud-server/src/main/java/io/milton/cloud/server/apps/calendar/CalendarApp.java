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
package io.milton.cloud.server.apps.calendar;

import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.event.JoinGroupEvent;
import io.milton.cloud.server.web.*;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class CalendarApp implements Application, EventListener {

    public static final String CALENDAR_HOME_NAME = "cal";
    private CalendarManager calendarManager;
    private ApplicationManager applicationManager;

    @Override
    public String getInstanceId() {
        return "calendar";
    }

    @Override
    public Resource getPage(Resource parent, String childName) {
        return null;
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        calendarManager = new CalendarManager();
        applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, JoinGroupEvent.class);
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof UserResource) {
            UserResource rf = (UserResource) parent;
            CalendarHomeFolder calHome = new CalendarHomeFolder(rf, CALENDAR_HOME_NAME, calendarManager);
            children.add(calHome);
        }

    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof JoinGroupEvent) {
            JoinGroupEvent joinEvent = (JoinGroupEvent) e;
            List<GroupInWebsite> giws = GroupInWebsite.findByGroup(joinEvent.getGroup(), SessionManager.session());
            for (GroupInWebsite giw  : giws) {
                if (applicationManager.isActive(this, giw.getWebsite())) {
                    addCalendar("cal", joinEvent.getProfile(), SessionManager.session());
                }
            }
        }
    }

    private void addCalendar(String name, Profile u, Session session) throws HibernateException {
        Calendar cal = new Calendar();
        cal.setOwner(u);
        cal.setCreatedDate(new Date());
        cal.setCtag(System.currentTimeMillis());
        cal.setModifiedDate(new Date());
        cal.setName(name);
        session.save(cal);
    }
}
