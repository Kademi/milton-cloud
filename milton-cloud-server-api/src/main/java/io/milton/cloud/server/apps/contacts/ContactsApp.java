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
import java.util.List;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class ContactsApp implements Application {

    public static final String ADDRESS_BOOK_HOME_NAME = "abs";
    
    private ContactManager contactManager;           
    private Services services;    
    private SpliffyResourceFactory resourceFactory;    
    
    @Override
    public Resource getPage(Resource parent, String childName) {
        return null;
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws IOException{
        this.services = resourceFactory.getServices();
        contactManager = new ContactManager();
        this.resourceFactory = resourceFactory;
//        SpliffyLdapTransactionManager txManager = new SpliffyLdapTransactionManager(resourceFactory.getSessionManager());                
//        Integer port = config.getInt("port");        
//        ldapServer = new LdapServer(txManager, this, resourceFactory.getPropertySources(), port, false, null);
//        ldapServer.start();
    }

    @Override
    public void shutDown() {
//        ldapServer.interrupt();
//        ldapServer.close();
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, List<Resource> children) {
        if( parent instanceof UserResource) {            
            UserResource rf = (UserResource) parent;
            ContactsHomeFolder calHome = new ContactsHomeFolder(rf, services, ADDRESS_BOOK_HOME_NAME, contactManager);
            children.add(calHome);
        }        
        
    }

//    @Override
//    public String getUserPassword(String userName) {
//        Session session = SessionManager.session();
//        Organisation rootOrg = OrganisationDao.getRootOrg(session);
//        Profile user = services.getSecurityManager().getUserDao().getProfile(userName, rootOrg, session);
//        if( user == null ) {
//            return null;
//        } else {
//            return services.getSecurityManager().getPasswordManager().getPassword(user);
//        }
//    }
//
//    @Override
//    public LdapPrincipal getUser(String userName, String password) {
//        Session session = SessionManager.session();
//        Organisation rootOrg = OrganisationDao.getRootOrg(session);
//        Profile user = (Profile) services.getSecurityManager().authenticate(rootOrg, userName, password);
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

    @Override
    public void initDefaultProperties(AppConfig config) {
        config.setInt("port", 8389); // default to non
    }
    @Override
    public void appendMenu(List<MenuItem> list, Resource r, Profile user, RootFolder rootFolder) {
        
    }


}

    
