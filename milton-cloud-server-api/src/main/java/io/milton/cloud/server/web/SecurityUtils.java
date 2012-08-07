package io.milton.cloud.server.web;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.principal.Principal;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class SecurityUtils {

//
//    private static void appendPermissions(BaseEntity grantee, Branch grantedOn, Session session, Set<Permission> perms) {
//        if (grantedOn.getPermissions() != null) {
//            BaseEntity repoEntity = grantedOn.getRepository().getBaseEntity();
//            Organisation repoOrg;
//            if( repoEntity instanceof Organisation) {
//                repoOrg = (Organisation) repoEntity;
//            } else {
//                repoOrg = repoEntity.getOrganisation();
//            }
//            for (Permission p : grantedOn.getPermissions()) {
//                if (p.isGrantedTo(grantee, repoOrg)) {
//                    perms.add(p);
//                }
//            }
//        }
//    }
//
//    private static void appendPermissions(BaseEntity grantee, BaseEntity grantedOn, Session session, Set<Permission> perms) {
//        if (grantedOn.getPermissions() != null) {
//            Organisation entityOrg;
//            if( grantedOn instanceof Organisation) {
//                entityOrg = (Organisation) grantedOn;
//            } else {
//                entityOrg = grantedOn.getOrganisation();
//            }
//            
//            for (Permission p : grantedOn.getPermissions()) {
//                if (p.isGrantedTo(grantee, entityOrg)) {
//                    perms.add(p);
//                }
//            }
//        }
//    }
//
//    public static Map<Principal, List<AccessControlledResource.Priviledge>> toMap(List<Permission> perms) throws NotAuthorizedException, BadRequestException {
//        Map<Principal, List<AccessControlledResource.Priviledge>> map = new HashMap<>();
//        if (perms != null) {
//            for (Permission p : perms) {
//                BaseEntity grantee = p.getGrantee();
//                if (grantee instanceof Profile) {
//                    // todo: handle groups
//                    Principal principal = SpliffyResourceFactory.getRootFolder().findEntity((Profile) grantee);
//                    List<AccessControlledResource.Priviledge> list = map.get(principal);
//                    if (list == null) {
//                        list = new ArrayList<>();
//                        map.put(principal, list);
//                    }
//                    list.add(p.getPriviledge());
//                }
//            }
//        }
//        return map;
//    }
//
//    public static void addPermissions(Collection<Permission> perms, List<Priviledge> list) {
//        for (Permission p : perms) {
//            list.add(p.getPriviledge());
//        }
//    }

    public static boolean hasWrite(List<Priviledge> privs) {
        for (Priviledge p : privs) {
            if (p.equals(Priviledge.WRITE)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasRead(List<Priviledge> privs) {
        for (Priviledge p : privs) {
            if (p.equals(Priviledge.READ)) {
                return true;
            }
        }
        return false;
    }
}
