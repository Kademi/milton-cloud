package io.milton.cloud.server.web;

import org.hibernate.Session;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.db.utils.UserDao;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.role.Role;
import io.milton.http.Auth;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.GroupRole;
import io.milton.vfs.db.utils.SessionManager;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author brad
 */
public class SpliffySecurityManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpliffySecurityManager.class);
    private String realm = "spliffy";
    private final UserDao userDao;
    private final PasswordManager passwordManager;
    private final Map<String, Role> mapOfRoles = new ConcurrentHashMap<>();

    public SpliffySecurityManager(UserDao userDao, PasswordManager passwordManager) {
        this.userDao = userDao;
        this.passwordManager = passwordManager;
    }

    public void add(Role role) {
        mapOfRoles.put(role.getName(), role);
    }

    public Profile getCurrentUser() {
        UserResource p = getCurrentPrincipal();
        if (p != null) {
            return p.getThisUser();
        }
        return null;
    }

    public UserResource getCurrentPrincipal() {
        if (HttpManager.request() == null) {
            log.warn("XXXXX   No current request  XXXXX");
            return null;
        }
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null || auth.getTag() == null) {
            log.warn("no auth object");
            return null;
        }
        UserResource ur = (UserResource) auth.getTag();
        if (ur == null) {
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

    public boolean authorise(Request req, Method method, Auth auth, CommonResource resource) {
        // look through all the user's groups to find one which permites this request
        Profile curUser = null;
        if (auth != null) {
            UserResource ur = (UserResource) auth.getTag();
            if (ur != null) {
                curUser = ur.getThisUser();
            }
        }
        Set<AccessControlledResource.Priviledge> privs = new HashSet<>();
        if (curUser != null) {
            if (curUser.getMemberships() != null) {
                for (GroupMembership m : curUser.getMemberships()) {
                    System.out.println("authorise: found membership: " + m.getGroupEntity().getName());
                    appendPriviledges(m.getGroupEntity(), m.getWithinOrg(), resource, privs);
                }
            }
            System.out.println("privs size: " + privs.size());
        }
        AccessControlledResource.Priviledge required = findRequiredPrivs(method, resource);
        boolean allows = containsPriviledge(required, privs);
        log.info("allows = " + allows);
        return allows;
    }

    public PasswordManager getPasswordManager() {
        return passwordManager;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    private void appendPriviledges(Group g, Organisation withinOrg, CommonResource resource, Set<AccessControlledResource.Priviledge> privs) {        
        if (g.getGroupRoles() != null) {
            System.out.println("appendPriviledges: group roles size: " + g.getGroupRoles().size() + " for group: " + g.getName());
            for (GroupRole gr : g.getGroupRoles()) {
                String roleName = gr.getRoleName();
                System.out.println("roleName: " + roleName);
                Role role = mapOfRoles.get(roleName);
                if (role != null) {
                    System.out.println("role: " + role);
                    if (role.appliesTo(resource, withinOrg)) {
                        System.out.println("does apply");
                        privs.addAll(role.getPriviledges());
                    } else {
                        System.out.println("does not apply");
                    }
                           
                } else {
                    log.warn("Role not found: " + roleName + " in roles: " + mapOfRoles.size());
                }
            }
        } else {
            System.out.println("appendPriviledges: no grs for: " + g.getName());
        }
    }


    /**
     * TODO: implement per method privs as in RFP -
     * http://tools.ietf.org/html/rfc3744
     *
     * @param method
     * @param resource
     * @return
     */
    private Priviledge findRequiredPrivs(Method method, CommonResource resource) {
        if (method.isWrite) {
            return Priviledge.WRITE;
        } else {
            return Priviledge.READ;
        }
    }

    /**
     * TODO: implement proper encapsulation, eg ALL contains READ,WRITE, READ
     * contains READ-CONTENT, etc
     *
     * @param required
     * @param privs
     * @return
     */
    private boolean containsPriviledge(Priviledge required, Set<Priviledge> privs) {
        for (Priviledge p : privs) {
            if (p.equals(Priviledge.ALL)) {
                return true;
            }
            if (p.equals(required)) {
                return true;
            }
        }
        return false;
    }
}
