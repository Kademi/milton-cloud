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
import io.milton.cloud.server.web.templating.MenuItem;
import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.Profile;

/**
 *
 * @author brad
 */
public class CalendarApp implements Application {

    public static final String CALENDAR_HOME_NAME = "cal";
    private CalendarManager calendarManager;
    private Services services;

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
        this.services = resourceFactory.getServices();
        calendarManager = new CalendarManager();
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof UserResource) {
            UserResource rf = (UserResource) parent;
            CalendarHomeFolder calHome = new CalendarHomeFolder(rf, services, CALENDAR_HOME_NAME, calendarManager);
            children.add(calHome);
        }

    }

    @Override
    public void initDefaultProperties(AppConfig config) {
    }

    @Override
    public void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder) {
        
    }

}
