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
package io.milton.cloud.server.apps.myfiles;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TextTemplater;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import org.apache.velocity.context.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.resource.CollectionResource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import java.io.IOException;

/**
 *
 * @author brad
 */
public class MyFilesApp implements Application, EventListener, PortletApplication, MenuApplication, BrowsableApplication {

    private ApplicationManager applicationManager;

    @Override
    public String getInstanceId() {
        return "myFiles";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "My files";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides end users with file storage, which they can syncronise with their own computers";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, SubscriptionEvent.class);
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof SubscriptionEvent) {
            SubscriptionEvent joinEvent = (SubscriptionEvent) e;
            Group group = joinEvent.getMembership().getGroupEntity();
            List<GroupInWebsite> giws = GroupInWebsite.findByGroup(group, SessionManager.session());
            for (GroupInWebsite giw : giws) {
                Branch b = giw.getWebsite().liveBranch();
                if (applicationManager.isActive(this, b)) {
                    Profile u = joinEvent.getMembership().getMember();
                    Session session = SessionManager.session();
                    addRepo("Documents", "docs", u, session);
                    addRepo("Music", "music", u, session);
                    addRepo("Pictures", "pics", u, session);
                    addRepo("Videos", "vids", u, session);
                }
            }
        }
    }

    private void addRepo(String title, String name, Profile u, Session session) throws HibernateException {
        Repository r1 = new Repository();
        r1.setBaseEntity(u);
        r1.setCreatedDate(new Date());
        r1.setName(name);
        r1.setTitle(title);
        session.save(r1);
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (portletSection.equals("dashboardPrimary")) {
            _(TextTemplater.class).writePage("myfiles/filesPortlet.html", currentUser, rootFolder, context, writer);
        }
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
                    String userHref = "/" + UserApp.USERS_FOLDER_NAME + "/" + curUser.getName() + "/";
                    for (Repository r : curUser.getRepositories()) {
                        if (r.type().equals("R")) { // dont handle specialised repo's like contacts
                            String repoHref = userHref + r.getName() + "/";
                            String title = r.getTitle() == null ? r.getName() : r.getTitle();
                            parent.getOrCreate("menu-myfiles-" + r.getName(), title, repoHref).setOrdering(50);
                        }
                    }
                    break;
            }
        }

    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof UserResource) {
            UserResource ur = (UserResource) parent;

        }
    }
}
