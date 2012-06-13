package io.milton.cloud.server.web;


import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Branch;
import io.milton.cloud.server.db.Permission;
import io.milton.cloud.server.db.Profile;
import io.milton.http.AccessControlledResource;
import io.milton.http.AccessControlledResource.Priviledge;
import io.milton.http.acl.Principal;
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

    public static Set<Permission> getPermissions(BaseEntity grantee, BaseEntity grantedOn, Session session) {
        Set<Permission> perms = new HashSet<>();
        appendPermissions(grantee, grantedOn, session, perms);
        return perms;
    }

    static Set<Permission> getPermissions(Profile grantee, Branch grantedOn, Session session) {
        Set<Permission> perms = new HashSet<>();
        appendPermissions(grantee, grantedOn, session, perms);
        return perms;
    }    

    private static void appendPermissions(BaseEntity grantee, Branch grantedOn, Session session, Set<Permission> perms) {
        if( grantedOn.getPermissions() != null ) {
            for( Permission p : grantedOn.getPermissions() ) {
                if( p.isGrantedTo(grantee)) {
                    perms.add(p);
                }
            }
        }
    }    
    
    private static void appendPermissions(BaseEntity grantee, BaseEntity grantedOn, Session session, Set<Permission> perms) {
        if( grantedOn.getPermissions() != null ) {
            for( Permission p : grantedOn.getPermissions() ) {
                if( p.isGrantedTo(grantee)) {
                    perms.add(p);
                }
            }
        }
    }

    public static Map<Principal, List<AccessControlledResource.Priviledge>> toMap(List<Permission> perms) {
        Map<Principal, List<AccessControlledResource.Priviledge>> map = new HashMap<>();
        if (perms != null) {
            for (Permission p : perms) {
                BaseEntity grantee = p.getGrantee();
                Principal principal = SpliffyResourceFactory.getRootFolder().findEntity(grantee.getName());
                List<AccessControlledResource.Priviledge> list = map.get(principal);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(principal, list);
                }
                list.add(p.getPriviledge());
            }
        }
        return map;
    }

    public static void addPermissions(Collection<Permission> perms, List<Priviledge> list) {
        for (Permission p : perms) {
            list.add(p.getPriviledge());
        }
    }

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
