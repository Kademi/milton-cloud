package io.milton.cloud.server.db;

import io.milton.http.AccessControlledResource;
import io.milton.http.AccessControlledResource.Priviledge;
import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

/**
 * Note that in this permission scheme priviledges can only be granted, not
 * revoked.
 *
 * This makes for a very simple permissions scheme that users can easily
 * understand and avoids the complexity of other file systems where users
 * frequently wonder why they can't access something, due to the complex
 * interaction between granted and revoked permissions in a hierarchy
 *
 * So, once a permission has been a applied to a certain folder, that permission
 * will apply on all resources under that folder
 *
 * A Permission can also be applied to an "entity" ie a user or organisation
 *
 * @author brad
 */
@Entity
public class Permission implements Serializable {

    public enum DynamicPrincipal {

        All,
        AUTHENTICATED,
        UNAUTHENTICATED
    }
    private long id;
    private AccessControlledResource.Priviledge priviledge;
    private BaseEntity grantedOnEntity;
    private BaseEntity grantee;
    private String granteePrincipal;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Priviledge getPriviledge() {
        return priviledge;
    }

    public void setPriviledge(Priviledge priviledge) {
        this.priviledge = priviledge;
    }

    /**
     * The physical, discrete, entity to convery the permission to.
     *
     * @return
     */
    @ManyToOne
    public BaseEntity getGrantee() {
        return grantee;
    }

    public void setGrantee(BaseEntity grantee) {
        this.grantee = grantee;
    }

    @ManyToOne
    public BaseEntity getGrantedOnEntity() {
        return grantedOnEntity;
    }

    public void setGrantedOnEntity(BaseEntity grantedOnEntity) {
        this.grantedOnEntity = grantedOnEntity;
    }

    /**
     * Permissions may be applied to non-discrete principals, such as a group
     * like "unauthenticated", which might indicate that anyone accessing the
     * resource would get that permission
     *
     * @return
     */
    public String getGranteePrincipal() {
        return granteePrincipal;
    }

    public void setGranteePrincipal(String granteePrincipal) {
        this.granteePrincipal = granteePrincipal;
    }

    /**
     * Checks the given entity and all of its group memberships to see if any
     * are associated with this permission
     *
     * @param entity - may be null, which indicated we're checking for allowed anonymous access
     * @return
     */
    public boolean isGrantedTo(BaseEntity entity) {
        if (getGrantedOnEntity() != null) {
            if (entity != null) {
                if (getGrantee().containsUser(entity)) {
                    return true;
                }
                List<GroupMembership> memberships = entity.getMemberships();
                if (memberships != null) {
                    for (GroupMembership m : memberships) {
                        Group g = m.getGroupEntity();
                        if (isGrantedTo(g)) {
                            return true;
                        }
                    }
                }
            }
        } else {
            if (granteePrincipal != null) {
                DynamicPrincipal p = DynamicPrincipal.valueOf(granteePrincipal);
                switch (p) {
                    case All:
                        return true;
                    case AUTHENTICATED:
                        return entity != null;
                    case UNAUTHENTICATED:
                        return entity == null;

                }
            }
        }
        return false;
    }
}
