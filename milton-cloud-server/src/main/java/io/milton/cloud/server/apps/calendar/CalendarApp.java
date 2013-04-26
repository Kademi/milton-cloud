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
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.DataResourceApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.contacts.ContactResource;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import static io.milton.context.RequestContext._;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.CalEvent;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class CalendarApp implements Application, EventListener, BrowsableApplication, MenuApplication, ChildPageApplication, DataResourceApplication {

    public static final String CALENDAR_HOME_NAME = "Calendars";
    private CalendarManager calendarManager;
    private ApplicationManager applicationManager;

    @Override
    public String getInstanceId() {
        return "calendar";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Calendars and events";
    }
    
    

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides users with calendars";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        calendarManager = new CalendarManager();
        applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, SubscriptionEvent.class);
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
        
        if (e instanceof SubscriptionEvent) {
            SubscriptionEvent joinEvent = (SubscriptionEvent) e;
            Group group = joinEvent.getMembership().getGroupEntity();
            List<GroupInWebsite> giws = joinEvent.getGroupInWebsites(group);
            for (GroupInWebsite giw  : giws) {
                Branch b = giw.getWebsite().liveBranch();
                if (joinEvent.isActive(applicationManager, this, b)) {
                    addCalendar("default", joinEvent.getMembership().getMember(), SessionManager.session());
                }
            }
        }
    }

    private void addCalendar(String name, Profile u, Session session) throws HibernateException {
        
        Calendar cal = u.calendar(name);
        if( cal != null ) {
            return ;
        }
        cal = new Calendar();
        cal.setBaseEntity(u);
        cal.setCreatedDate(new Date());
        cal.setName(name);
        cal.setTitle(name);
        Repository.initRepo(cal, name, session, u, u);
        u.getCalendars().add(cal);
        session.save(cal);
        
    
    }

    @Override
    public void appendMenu(MenuItem parent) {
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (curUser == null) {
            return;
        }
        if (parent.getRootFolder() instanceof WebsiteRootFolder) {
            switch (parent.getId()) {
                case "menuRoot":
                    parent.getOrCreate("menu-mycalendars", "Calendars", "/mycalendars").setOrdering(50);
                    break;
            }
        }

    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if( parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            if( requestedName.equals("mycalendars")) {
                return new MyCalendarsPage(requestedName, wrf);
            }                
        } else if( parent instanceof CalendarFolder ) {
            if( requestedName.equals("new")) {
                CalendarFolder cal = (CalendarFolder) parent;
                return new CalEventResource(null, cal, null, calendarManager);
            }
        }
        return null;
    }

    @Override
    public ContentResource instantiateResource(Object dm, CommonCollectionResource parent, RootFolder rf) {
        if (parent instanceof CalendarFolder) {
            if (dm instanceof DataSession.FileNode) {
                DataSession.FileNode fn = (DataSession.FileNode) dm;
                CalendarFolder calendarFolder = (CalendarFolder) parent;
                CalEvent c = calendarFolder.getCalendar().event(fn.getName());
                return new CalEventResource(fn, calendarFolder, c, calendarManager);
            }
        }
        return null;
    }
}
