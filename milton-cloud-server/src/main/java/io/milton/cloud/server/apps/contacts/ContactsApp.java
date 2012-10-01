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
package io.milton.cloud.server.apps.contacts;

import java.io.IOException;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.DataResourceApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.user.UserApp;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.resource.CollectionResource;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Contact;

/**
 *
 * @author brad
 */
public class ContactsApp implements Application, EventListener, BrowsableApplication, MenuApplication, DataResourceApplication {

    public static final String ADDRESS_BOOK_HOME_NAME = "abs";
    private ContactManager contactManager;
    private ApplicationManager applicationManager;
    private SpliffyResourceFactory resourceFactory;

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Contacts and address books";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides end users with address books, which can be accessed and synced from email clients and mobile devices";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws IOException {
        contactManager = new ContactManager();
        this.resourceFactory = resourceFactory;
        this.applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, SubscriptionEvent.class);
//        SpliffyLdapTransactionManager txManager = new SpliffyLdapTransactionManager(resourceFactory.getSessionManager());                
//        Integer port = config.getInt("port");        
//        ldapServer = new LdapServer(txManager, this, resourceFactory.getPropertySources(), port, false, null);
//        ldapServer.start();
    }

//    @Override
//    public void shutDown() {
//        ldapServer.interrupt();
//        ldapServer.close();
//    }
    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof UserResource) {
            UserResource rf = (UserResource) parent;
            ContactsHomeFolder calHome = new ContactsHomeFolder(rf, ADDRESS_BOOK_HOME_NAME, contactManager);
            children.add(calHome);
        }

    }

//    @Override
//    public String getUserPassword(String userName) {
//        Session session = SessionManager.session();
//        Organisation rootOrg = OrganisationDao.getRootOrg(session);
//        Profile user = _(SpliffySecurityManager.class).getUserDao().getProfile(userName, rootOrg, session);
//        if( user == null ) {
//            return null;
//        } else {
//            return _(SpliffySecurityManager.class).getPasswordManager().getPassword(user);
//        }
//    }
//
//    @Override
//    public LdapPrincipal getUser(String userName, String password) {
//        Session session = SessionManager.session();
//        Organisation rootOrg = OrganisationDao.getRootOrg(session);
//        Profile user = (Profile) _(SpliffySecurityManager.class).authenticate(rootOrg, userName, password);
//        if( user == null) {
//            return null;
//        }
//        Organisation org = OrganisationDao.getRootOrg(SessionManager.session());
//        OrganisationFolder rf = new OrganisationFolder(services, resourceFactory.getApplicationManager(), org);
//        return rf.findEntity(userName);
//    }
//
//    @Override
//    public List<LdapContact> galFind(Condition equalTo, int sizeLimit) {
//        return Collections.EMPTY_LIST;
//    }
    @Override
    public String getInstanceId() {
        return "contacts";
    }

//    @Override
//    public void initDefaultProperties(AppConfig config) {
//        config.setInt("port", 8389); // default to non
//    }
    @Override
    public void onEvent(Event e) {
        if (e instanceof SubscriptionEvent) {
            SubscriptionEvent joinEvent = (SubscriptionEvent) e;
            Group group = joinEvent.getMembership().getGroupEntity();
            List<GroupInWebsite> giws = GroupInWebsite.findByGroup(group, SessionManager.session());
            for (GroupInWebsite giw : giws) {
                Branch b = giw.getWebsite().liveBranch();
                if (applicationManager.isActive(this, b)) {
                    addAddressBook("contact", joinEvent.getMembership().getMember(), SessionManager.session());
                }
            }
        }
    }

    private void addAddressBook(String name, Profile u, Session session) throws HibernateException {
        AddressBook addressBook = new AddressBook();
        addressBook.setName(name);
        addressBook.setTitle(name);
        addressBook.setBaseEntity(u);
        addressBook.setCreatedDate(new Date());
        session.save(addressBook);
    }

    @Override
    public void appendMenu(MenuItem parent) {
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (curUser == null) {
            return;
        }
        switch (parent.getId()) {
            case "menuRoot":
                String userHref = "/" + UserApp.USERS_FOLDER_NAME + "/" + curUser.getName() + "/";
                for (Repository r : curUser.getRepositories()) {
                    if (r instanceof AddressBook) {
                        String repoHref = userHref + ADDRESS_BOOK_HOME_NAME + "/" + r.getName() + "/";
                        String title = r.getTitle() == null ? r.getName() : r.getTitle();
                        parent.getOrCreate("menu-mycontacts-" + r.getName(), title, repoHref).setOrdering(50);
                    }
                }
                break;
        }
    }

    @Override
    public ContentResource instantiateResource(Object dm, CommonCollectionResource parent, RootFolder rf) {
        if (parent instanceof ContactsFolder) {
            if (dm instanceof DataSession.FileNode) {
                DataSession.FileNode fn = (DataSession.FileNode) dm;
                ContactsFolder contactsFolder = (ContactsFolder) parent;
                Contact c = contactsFolder.getAddressBook().contact(fn.getName());
                return new ContactResource(fn, contactsFolder, c, contactManager);
            }
        }
        return null;
    }
}
