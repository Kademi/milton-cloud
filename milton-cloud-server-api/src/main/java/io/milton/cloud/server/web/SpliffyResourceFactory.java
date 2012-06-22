package io.milton.cloud.server.web;

import com.ettrema.common.Service;
import io.milton.cloud.server.db.utils.UserDao;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.common.Path;
import io.milton.event.EventManager;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class SpliffyResourceFactory implements ResourceFactory, Service {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffyResourceFactory.class);

    public static RootFolder getRootFolder() {
        if (HttpManager.request() != null) {
            return (RootFolder) HttpManager.request().getAttributes().get("_spliffy_root_folder");
        } else {
            return null;
        }
    }
    private final UserDao userDao;
    private final SpliffySecurityManager securityManager;
    private final Services services;
    private final ApplicationManager applicationManager;
    private final EventManager eventManager;
    private final SessionManager sessionManager;

    public SpliffyResourceFactory(UserDao userDao, SpliffySecurityManager securityManager, Services services, ApplicationManager applicationManager, EventManager eventManager, SessionManager sessionManager) {
        this.userDao = userDao;
        this.securityManager = securityManager;
        this.services = services;
        this.applicationManager = applicationManager;
        this.eventManager = eventManager;
        this.sessionManager = sessionManager;
    }

    @Override
    public void start() {
        applicationManager.init(this);
    }

    @Override
    public void stop() {
        applicationManager.shutDown();
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        Path path = Path.path(sPath);
        Resource r = find(host, path);
        if( r != null ) {
            System.out.println("Got resource: " + r.getClass());
        }
        return r;
    }

    private Resource find(String host, Path p) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        if (p.isRoot()) {
            Resource rootFolder = (Resource) HttpManager.request().getAttributes().get("_spliffy_root_folder");
            if (rootFolder == null) {
                rootFolder = applicationManager.getPage(null, host);
                HttpManager.request().getAttributes().put("_spliffy_root_folder", rootFolder);
            }
            return rootFolder;
        } else {
            Resource rParent = find(host, p.getParent());
            if (rParent == null) {
                return null;
            } else {
                if (rParent instanceof CollectionResource) {
                    CollectionResource parent = (CollectionResource) rParent;
                    return parent.child(p.getName());
                } else {
                    return null;
                }
            }
        }
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public SpliffySecurityManager getSecurityManager() {
        return securityManager;
    }

    public Services getServices() {
        return services;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
