package io.milton.cloud.server.web;

import java.util.List;
import org.hibernate.Session;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.db.utils.UserDao;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class SpliffySecurityManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffySecurityManager.class);
    private String realm = "spliffy";
    private final UserDao userDao;
    private final PasswordManager passwordManager;

    public SpliffySecurityManager(UserDao userDao, PasswordManager passwordManager) {
        this.userDao = userDao;
        this.passwordManager = passwordManager;
    }

    public Profile getCurrentUser() {
        UserResource p = getCurrentPrincipal();
        if (p != null) {
            return p.getThisUser();
        }
        return null;
    }

    public UserResource getCurrentPrincipal() {
        if( HttpManager.request() == null ) {
            log.warn("XXXXX   No current request  XXXXX");
            return null;
        }
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null || auth.getTag() == null) {
            log.warn("no auth object");
            return null;
        }
        UserResource ur = (UserResource) auth.getTag();
        if( ur == null ) {
            log.warn("Got auth object but null tag");
        }
        return ur;
    }

    public Profile authenticate(Organisation org, String userName, String requestPassword) {
        Session session = SessionManager.session();
        Profile user = userDao.findProfile(userName, org, session);
        if (user == null) {
            log.warn("user not found: " + userName);
            return null;
        } else {
            // only the password hash is stored on the user, so need to generate an expected hash
            if (passwordManager.verifyPassword(user, requestPassword)) {
                //HttpManager.request().getAttributes().put("_current_user", user);
                return user;
            } else {
                return null;
            }
        }
    }

    public Profile authenticate(Organisation org, DigestResponse digest) {
        log.trace("authenticate: " + digest.getUser());
        Session session = SessionManager.session();
        Profile user = userDao.findProfile(digest.getUser(), org, session);
        while (user == null && org != null) {
            org = org.getOrganisation();
            user = userDao.getProfile(digest.getUser(), org, session);
        }
        if (user == null) {
            log.warn("user not found: " + digest.getUser());
            return null;
        }
        if (passwordManager.verifyDigest(digest, user)) {
//            log.info("digest auth ok: " + user.getName());
            //HttpManager.request().getAttributes().put("_current_user", user);
            return user;
        } else {
            log.warn("password verifuication failed");
            return null;
        }
    }

    public String getRealm() {
        return realm;
    }

    public boolean authorise(Request req, Method method, Auth auth, Resource aThis) {
        if (aThis instanceof AccessControlledResource) {
            System.out.println("is acr");
            AccessControlledResource acr = (AccessControlledResource) aThis;
            List<Priviledge> privs = acr.getPriviledges(auth);
            boolean result;
            if (method.isWrite) {
                //result = SecurityUtils.hasWrite(privs);

                // Currently doesnt give administrators (ie users defined on parent orgs) access
                result = (auth != null && auth.getTag() != null);
            } else {
                result = SecurityUtils.hasRead(privs);
                if (!result) {
                    if (auth != null) {
                        System.out.println("override result");
                        result = auth.getTag() != null;
                    }
                }
            }
            System.out.println("result: " + result);
            if (!result) {
                if (auth != null && auth.getTag() != null) {
                    log.info("Denied access of: " + auth + " to resource: " + aThis.getName() + " (" + aThis.getClass() + ") because of authorisation failure");
                    log.info("Requires " + (method.isWrite ? "writable" : "read") + "access");
                    log.info("Allowed privs of current user are:");
                    for (Priviledge p : privs) {
                        log.info("   - " + p);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("stack trace so you know whats going on", new Exception("not a real exception"));
                    }
                } else {
                    log.info("Authorisation declined, not logged in");
                }
            }
            return result;
        } else {
            return true; // not access controlled so must be ok!
        }
    }

    public PasswordManager getPasswordManager() {
        return passwordManager;
    }

    public UserDao getUserDao() {
        return userDao;
    }
}
