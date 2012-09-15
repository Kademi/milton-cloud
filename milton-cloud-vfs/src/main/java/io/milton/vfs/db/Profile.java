package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    public static Profile find(String name, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        crit.add(
                Restrictions.disjunction()
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("email", name)));
        return DbUtils.unique(crit);
    }

    public static Profile find(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        // join to group membership, then subordinate, then restrict on org        
        Criteria critMembership = crit.createCriteria("memberships");
        Criteria critSubordinate = critMembership.createCriteria("subordinates");
        crit.add(Restrictions.eq("name", name));
        critSubordinate.add(Restrictions.eq("withinOrg", org));
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            log.warn("Profile not found: " + name + " in org: " + org.getName());
            return null;
        } else {
            return (Profile) list.get(0);
        }
    }

    public static List<Profile> findByBusinessUnit(Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        // join to group membership, then subordinate, then restrict on org        
        Criteria critMembership = crit.createCriteria("memberships");
        Criteria critSubordinate = critMembership.createCriteria("subordinates");
        critSubordinate.add(Restrictions.eq("withinOrg", organisation));
        return DbUtils.toList(crit, Profile.class);
    }

    public static Profile findByEmail(String email, Organisation org, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        // join to group membership, then subordinate, then restrict on org        
        Criteria critMembership = crit.createCriteria("memberships");
        Criteria critSubordinate = critMembership.createCriteria("subordinates");
        crit.add(Restrictions.eq("email", email));
        critSubordinate.add(Restrictions.eq("withinOrg", org));
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return (Profile) list.get(0);
        }

    }

    public static Profile get(long profileId, Session session) {
        return (Profile) session.get(Profile.class, profileId);
    }

    /**
     * Find a unique name based on the given base name
     *
     * @param nickName
     * @return
     */
    public static  String findUniqueName(String nickName, Session session) {
        String candidateName = nickName;
        int counter = 1;
        while (!isUniqueName(candidateName, session)) {
            candidateName = nickName + counter++;
        }
        return candidateName;
    }

    public static boolean isUniqueName(String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Expression.eq("name", name));
        return crit.uniqueResult() == null;
    }

    
    private List<Credential> credentials;
    private List<GroupMembership> memberships; // can belong to groups    
    private String firstName;
    private String surName;
    private String phone;
    private String email;
    private String photoHash;
    private String nickName;
    private boolean enabled;
    private boolean rejected;

    @Override
    public void delete(Session session) {
        if (getMemberships() != null) {
            for (GroupMembership m : getMemberships()) {
                session.delete(m);
            }
            setMemberships(null);
        }
        super.delete(session);
    }

    @OneToMany(mappedBy = "profile")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    /**
     * Returns the groups that this entity is a member of. Not to be confused
     * with members on a group, which lists the entities which are in the group
     *
     * @return
     */
    @OneToMany(mappedBy = "member")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<GroupMembership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<GroupMembership> memberships) {
        this.memberships = memberships;
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

    public String getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(String photoHash) {
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

    /**
     * Create a GroupMembership linking this profile to the given group, within the given
     * organisation. Is immediately saved
     * 
     * @param g
     * @return 
     */

    public Profile addToGroup(Group g, Organisation hasGroupInOrg, Session session) {
        if( g.isMember(this, hasGroupInOrg)) {
            return this;
        }
        GroupMembership gm = new GroupMembership();
        gm.setCreatedDate(new Date());
        gm.setGroupEntity(g);
        gm.setMember(this);
        gm.setWithinOrg(hasGroupInOrg);
        gm.setModifiedDate(new Date());
        session.save(gm);
        
        // Need to create a subordinate record for each parent organisation
        Organisation subordinateTo = hasGroupInOrg;
        while(subordinateTo != null ) {
            createSubordinate(subordinateTo, gm, session);
            subordinateTo = subordinateTo.getOrganisation();
        }
        
        return this;
    }  
    
    /**
     * Creates a Subordinate record
     * 
     * @param subordinateTo
     * @param gm 
     */
    private void createSubordinate(Organisation subordinateTo, GroupMembership gm, Session session) {
        Subordinate s = new Subordinate();
        s.setWithinOrg(subordinateTo);
        s.setGroupMembership(gm);
        session.save(s);
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
                    for (GroupRole r : m.getGroupEntity().getGroupRoles()) {
                        if (r.getRoleName().equals(roleName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Transient
    public List<AddressBook> getAddressBooks() {
        List<AddressBook> list = new ArrayList();
        if( getRepositories() != null ) {
            for( Repository r : getRepositories() ) {
                if( r instanceof AddressBook ) {
                    AddressBook ab = (AddressBook) r;
                    list.add(ab);
                }
            }
        }
        return list;
    }

    @Transient
    public String getFormattedName() {
        String name = "";
        if( getFirstName() != null && getFirstName().length() > 0 ) {
            name += getFirstName();
        }
        if( getSurName() != null && getSurName().length() > 0 ) {
            name = name + " " + getSurName();
        }
        return name;
    }
}
