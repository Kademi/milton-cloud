package io.milton.cloud.server.web;

import org.hibernate.Session;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.db.utils.UserDao;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.role.Role;
import io.milton.http.AclUtils;
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
import java.util.Collection;
import java.util.Collections;
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
    
    private String publicGroup = "public";

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
        Profile user = Profile.find(userName, session);
        if (user == null) {
            log.warn("user not found: " + userName);
            return null;
        } else {
            log.info("verify password for: " + user.getName());
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
        Profile user = Profile.find(digest.getUser(), session);
        while (user == null && org != null) {
            org = org.getOrganisation();
            user = Profile.find(org, digest.getUser(), session);
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
        Set<AccessControlledResource.Priviledge> privs = getPriviledges(curUser, resource);
        AccessControlledResource.Priviledge required = findRequiredPrivs(method, resource);
        boolean allows = AclUtils.containsPriviledge(required, privs);
        if( !allows ) {
            if( curUser != null ) {
                log.info("Authorisation declined for user: " + curUser.getName() );
            } else {
                log.info("Authorisation declined for anonymous access");
            }
            log.info("Required priviledge: " + required + " was not found in assigned priviledge list of size: " + privs.size());
        }
        log.info("allows = " + allows + " rsource: " + resource.getClass());
        return allows;
    }

    public PasswordManager getPasswordManager() {
        return passwordManager;
    }

    public UserDao getUserDao() {
        return userDao;
    }
    
    public Set<AccessControlledResource.Priviledge> getPriviledges(Profile curUser, CommonResource resource) {
        Set<AccessControlledResource.Priviledge> privs = new HashSet<>();
        if (curUser != null) {
            if (curUser.getMemberships() != null) {
                for (GroupMembership m : curUser.getMemberships()) {
                    appendPriviledges(m.getGroupEntity(), m.getWithinOrg(), resource, privs);
                }
            }
            System.out.println("privs size: " + privs.size());
        } else {
            Organisation org = resource.getOrganisation();
            Group pg = org.group(publicGroup, SessionManager.session());
            if( pg != null ) {
                appendPriviledges(pg, org, resource, privs);
            }
        }
        return privs;
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
                    if (role.appliesTo(resource, withinOrg, g)) {
                        System.out.println("does apply");
                        privs.addAll(role.getPriviledges(resource, withinOrg, g));
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
        if( method.equals(Method.POST)) {
            return Priviledge.READ_CONTENT; // generally POST is just an interactive part of consuming content
        } else if (method.isWrite) {            
            return Priviledge.WRITE;
        } else {
            return Priviledge.READ;
        }
    }

    
    public Collection<Role> getGroupRoles() {
        return Collections.unmodifiableCollection(mapOfRoles.values());        
    }
}
