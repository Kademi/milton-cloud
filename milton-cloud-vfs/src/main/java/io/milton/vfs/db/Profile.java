package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Expression;

/**
 * A user profile is defined within an organisation. Might change this in the
 * future so that the user profile is within an organsiation, but the
 * credentials probably should exist in a global space.
 *
 * @author brad
 */
@javax.persistence.Entity
@DiscriminatorValue("U")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Profile extends BaseEntity implements VfsAcceptor {

    public static List<Profile> findByBusinessUnit(Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.add(Expression.eq("organisation", organisation));
        return DbUtils.toList(crit, Profile.class);
    }

    public static Profile findByEmail(String email, Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.add(Expression.eq("organisation", organisation));
        crit.add(Expression.eq("email", email));
        return DbUtils.unique(crit);
    }

    public static Profile get(long profileId, Session session) {
        return (Profile) session.get(Profile.class, profileId);
    }
    private List<Credential> credentials;
    private String firstName;
    private String surName;
    private String phone;
    private String email;
    private long photoHash;
    private String nickName;
    private boolean enabled;
    private boolean rejected;

    @OneToMany(mappedBy = "profile")
    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column
    @Index(name = "ids_profile_email")
    public String getEmail() {
        return email;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column
    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(long photoHash) {
        this.photoHash = photoHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * True means the user can login
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    @Override
    public Profile addToGroup(Group g, Organisation hasGroupInOrg) {
        return (Profile) super.addToGroup(g, hasGroupInOrg);
    }

    public boolean isInGroup(String groupName, Organisation org) {
        if (getMemberships() != null) {
            for (GroupMembership m : getMemberships()) {
                if (m.getGroupEntity().getName().equals(groupName)) {
                    if (org.isWithin(m.getWithinOrg())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasRole(String roleName, Organisation org) {
        if (getMemberships() != null) {
            for (GroupMembership m : getMemberships()) {
                if (org.isWithin(m.getWithinOrg())) {
                    for (GroupRole r  : m.getGroupEntity().getGroupRoles()) {
                        if( r.getRoleName().equals(roleName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
