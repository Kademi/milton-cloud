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
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.Website;
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
            Profile profile = p.getThisUser();
            //log.info("current user: " + profile);
            return profile;
        }
        //log.info("no current user");
        return null;
    }

    public UserResource getCurrentPrincipal() {
        if (HttpManager.request() == null) {
            return null;
        }
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null || auth.getTag() == null) {
            //log.warn("no auth object");
            return null;
        }
        if (auth.getTag() instanceof UserResource) {
            UserResource ur = (UserResource) auth.getTag();
            if (ur == null) {
                log.warn("Got auth object but null tag");
            }

            return ur;
        }
        return null;
    }

    public void setCurrentPrincipal(UserResource p) {
        log.info("setCurrentPrincipal: " + p);
        Auth auth = HttpManager.request().getAuthorization();
        if (auth == null) {
            auth = new Auth(p.getName(), p);
            HttpManager.request().setAuthorization(auth);
        } else {
            auth.setTag(p);
        }
    }

    public Profile authenticate(Organisation org, String userName, String requestPassword) {
        Session session = SessionManager.session();
        Profile user = Profile.find(userName, session);
        if (user == null) {
            log.warn("user not found: " + userName);
            return null;
        } else {
            // only the password hash is stored on the user, so need to generate an expected hash
            if (passwordManager.verifyPassword(user, requestPassword)) {
                // Now make sure that the user has an account within this org or a parent org
                Organisation checkOrg = org;
                while (checkOrg != null && !checkOrg.containsUser(user, session)) {
                    checkOrg = checkOrg.getOrganisation();
                }
                if (checkOrg == null) {
                    log.warn("Profile " + user.getName() + " exists, but it not subordinate to org: " + org.getOrgId());
                    return null;
                } else {
                    return user;
                }
            } else {
                return null;
            }
        }
    }

    public Profile authenticate(Organisation org, DigestResponse digest) {
        Session session = SessionManager.session();
        Profile user = Profile.find(digest.getUser(), session);
        if (user == null) {
            log.warn("user not found: " + digest.getUser());
            return null;
        } else {
            // only the password hash is stored on the user, so need to generate an expected hash
            if (passwordManager.verifyDigest(digest, user)) {
                // Now make sure that the user has an account within this org or a parent org
                Organisation checkOrg = org;
                while (checkOrg != null && !checkOrg.containsUser(user, session)) {
                    checkOrg = checkOrg.getOrganisation();
                }
                if (checkOrg == null) {
                    log.warn("Profile " + user.getName() + " exists, but it not subordinate to org: " + org.getOrgId());
                    return null;
                } else {
                    return user;
                }
            } else {
                log.warn("digest password verification failed");
                return null;
            }
        }
    }

    public String getRealm() {
        return realm;
    }

    public boolean authorise(Request req, Method method, Auth auth, CommonResource resource) {
        if (log.isTraceEnabled()) {
            log.trace("authorise: method=" + method + " resource=" + resource);
        }
        // look through all the user's groups to find one which permites this request
        Profile curUser = null;
        if (auth != null) {
            UserResource ur = (UserResource) auth.getTag();
            if (ur != null) {
                curUser = ur.getThisUser();
            }
        }
        Set<AccessControlledResource.Priviledge> privs = getPriviledges(curUser, resource);
        Set<Priviledge> expanded = AclUtils.expand(privs);
        if (log.isDebugEnabled()) {
            log.debug("expanded privs: " + expanded);
        }
        req.getAttributes().put("privs", expanded); // stash them for later, page rendering might be interested
        AccessControlledResource.Priviledge required = findRequiredPriv(method, resource, req);
        boolean allows;
        if (required == null) {
            if (log.isTraceEnabled()) {
                log.trace("allowing access because no privs are request for " + method + " on resource: " + resource);
            }
            allows = true;
        } else {
            allows = AclUtils.containsPriviledge(required, privs);
            if (!allows) {
                if (curUser != null) {
                    log.info("Authorisation declined for user: " + curUser.getName());
                } else {
                    log.info("Authorisation declined for anonymous access");
                }
                log.info("Required priviledge: " + required + " was not found in assigned priviledge list of size: " + privs.size());
            }
            if (log.isTraceEnabled()) {
                log.trace("allows = " + allows + " rsource: " + resource.getClass() + " required=" + required + " found privs=" + privs.size());
            }
        }
        return allows;
    }

    public PasswordManager getPasswordManager() {
        return passwordManager;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public Set<Group> getGroups(Profile p, Website website) {
        if (p == null) {
            return null;
        }
        Set<Group> set = new HashSet<>();
        if (p.getMemberships() != null) {
            for (GroupMembership gm : p.getMemberships()) {
                if (website.hasGroup(gm.getGroupEntity(), SessionManager.session())) {
                    set.add(gm.getGroupEntity());
                }
            }
        }
        return set;
    }

    public Set<AccessControlledResource.Priviledge> getPriviledges(Profile curUser, CommonResource resource) {
        Set<AccessControlledResource.Priviledge> privs = new HashSet<>();
        if (resource.isPublic()) {
            if (log.isTraceEnabled()) {
                log.trace("getPriviledges: granting read access because resource is public: " + resource.getClass());
            }
            privs.add(Priviledge.READ_CONTENT);
        }

        if (curUser != null) {
            if (log.isTraceEnabled()) {
                log.trace("getPriviledges: get privs for user: " + curUser.getName());
            }
            // If the resource is a content resource and the current user is the direct owner of the repository, then grant R/W
            if (resource instanceof PersonalResource) {
                PersonalResource cr = (PersonalResource) resource;
                Profile owner = cr.getOwnerProfile();
                if (owner != null && owner == curUser) {
                    privs.addAll(Role.READ_WRITE);
                }
            }

            if (curUser.getMemberships() != null && !curUser.getMemberships().isEmpty()) {
                for (GroupMembership m : curUser.getMemberships()) {
                    if (log.isTraceEnabled()) {
                        log.trace("getPriviledges: append privs for group membership: " + m.getGroupEntity().getName());
                    }
                    appendPriviledges(m.getGroupEntity(), m.getWithinOrg(), resource, privs);
                }
            } else {
                log.trace("getPriviledges: user has no group memberships");
            }
        } else {
            Organisation org = resource.getOrganisation();
            Group pg = org.group(publicGroup, SessionManager.session());
            if (pg != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Found public group, so granting privs from: " + pg.getName());
                }
                appendPriviledges(pg, org, resource, privs);
            }
        }
        return privs;
    }

    private void appendPriviledges(Group g, Organisation withinOrg, CommonResource resource, Set<AccessControlledResource.Priviledge> privs) {
        if (g.getGroupRoles() != null) {
            for (GroupRole gr : g.getGroupRoles()) {
                String roleName = gr.getRoleName();
                Role role = mapOfRoles.get(roleName);
                Organisation checkOrg = withinOrg;
                if (gr.getWithinOrg() != null) {
                    checkOrg = gr.getWithinOrg();
                }
                if (role != null) {
                    Repository applicableRepo = gr.getRepository();
                    if (applicableRepo != null) {
                        // role applies to a repository, so check if current resource is within the applicable repo
                        if (role.appliesTo(resource, applicableRepo, g)) {
                            Set<Priviledge> privsToAdd = role.getPriviledges(resource, applicableRepo, g);
                            privs.addAll(privsToAdd);
                        }
                    } else {
                        if (role.appliesTo(resource, checkOrg, g)) {
                            Set<Priviledge> privsToAdd = role.getPriviledges(resource, checkOrg, g);
                            if (log.isTraceEnabled()) {
                                log.trace("role:" + roleName + " does apply to: " + checkOrg.getOrgId() + ", add privs " + privsToAdd);
                            }
                            privs.addAll(privsToAdd);
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace("role:" + roleName + " does not apply to: " + checkOrg.getOrgId() + " - " + role);
                            }
                        }
                    }

                } else {
                    log.warn("Role not found: " + roleName + " in roles: " + mapOfRoles.size());
                }
            }
        } else {
            //System.out.println("appendPriviledges: no grs for: " + g.getName());
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
    private Priviledge findRequiredPriv(Method method, CommonResource resource, Request request) {
        if (method.equals(Method.POST)) {
            Priviledge p = resource.getRequiredPostPriviledge(request);
            if (p == null) {
                p = Priviledge.WRITE;
            }
            return p;
        } else if (method.equals(Request.Method.UNLOCK)) {
            return Priviledge.UNLOCK;
        } else if (method.isWrite) {
            return Priviledge.WRITE_CONTENT;
        } else {
            return Priviledge.READ_CONTENT;
        }
    }

    public Collection<Role> getGroupRoles() {
        return Collections.unmodifiableCollection(mapOfRoles.values());
    }
}
