package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
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
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name"})})
public class Profile extends BaseEntity implements VfsAcceptor {

    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    public static String findAutoName(String nickName, Session session) {
        String nameToUse = DbUtils.replaceYuckyChars(nickName);
        nameToUse = getUniqueName(nameToUse, session);
        return nameToUse;
    }

    public static String getUniqueName(final String baseName, Session session) {
        String name = baseName;
        Profile r = find(name, session);
        int cnt = 0;
        boolean isFirst = true;
        while (r != null) {
            cnt++;
            name = DbUtils.incrementFileName(name, isFirst);
            isFirst = false;
            r = find(name, session);
        }
        return name;
    }

    /**
     *
     * @param baseName
     * @param parameters
     * @param folder
     * @return
     */
    public static String getImpliedName(Map<String, String> parameters) {
        String nameToCreate;

        if (parameters.containsKey("name")) {
            nameToCreate = parameters.get("name");
        } else if (parameters.containsKey("nickName")) {
            nameToCreate = parameters.get("nickName");
        } else if (parameters.containsKey("fullName")) {
            nameToCreate = parameters.get("fullName");
        } else if (parameters.containsKey("firstName")) {
            String fullName = parameters.get("firstName");
            if (parameters.containsKey("surName")) {
                fullName = fullName + "." + parameters.get("surName");
            }
            nameToCreate = fullName;
        } else if (parameters.containsKey("title")) {
            nameToCreate = parameters.get("title");
        } else {
            nameToCreate = "$[counter]";
        }

        return nameToCreate;
    }

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
            log.warn("Profile not found: " + name + " in org: " + org.getOrgId());
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
        return DbUtils.unique(crit);
    }

    public static Profile findByEmail(String email, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("email", email));
        return DbUtils.unique(crit);
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
    public static String findUniqueName(String nickName, Session session) {
        String candidateName = nickName;
        int counter = 1;
        while (!isUniqueName(candidateName, session)) {
            candidateName = nickName + counter++;
        }
        return candidateName;
    }

    public static boolean isUniqueName(String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Restrictions.eq("name", name));
        return crit.uniqueResult() == null;
    }
    private String name;
    private String firstName;
    private String surName;
    private String phone;
    private String email;
    private String photoHash;
    private String nickName;
    private boolean enabled;
    private boolean rejected;
    private List<Credential> credentials;
    private List<GroupMembership> memberships; // can belong to groups    

    @Column(nullable = false)
    @Index(name = "ids_entity_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public GroupMembership membership(Group group) {
        if (getMemberships() != null) {
            for (GroupMembership gm : getMemberships()) {
                if (gm.getGroupEntity() == group) {
                    return gm;
                }
            }
        }
        return null;
    }

    public void removeMembership(Group group, Session session) {
        if (getMemberships() != null) {
            Iterator<GroupMembership> it = getMemberships().iterator();
            List<GroupMembership> toRemove = new ArrayList<>();
            while (it.hasNext()) {
                GroupMembership gm = it.next();
                if (gm.getGroupEntity() == group) {
                    System.out.println("found a GM to remove from profile: " + getEmail());
                    toRemove.add(gm);
                }
            }
            if( !toRemove.isEmpty() ) {
                for( GroupMembership gm : toRemove ) {
                    gm.delete(session);
                }
                session.flush();
            }
        }
    }

    /**
     * Create a GroupMembership linking this profile to the given group, within
     * the given organisation. Is immediately saved
     *
     * @param g
     * @return
     */
    public Profile addToGroup(Group g, Organisation hasGroupInOrg, Session session) {
        if (g.isMember(this, hasGroupInOrg, session)) {
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
        while (subordinateTo != null) {
            createSubordinate(subordinateTo, gm, session);
            subordinateTo = subordinateTo.getOrganisation();
        }

        if (getMemberships() == null) {
            setMemberships(new ArrayList<GroupMembership>());
        }
        getMemberships().add(gm);
        if (g.getGroupMemberships() == null) {
            g.setGroupMemberships(new ArrayList<GroupMembership>());
        }
        g.getGroupMemberships().add(gm);

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
        if( gm.getSubordinates() == null ) {
            gm.setSubordinates(new ArrayList<Subordinate>());
        }
        gm.getSubordinates().add(s);
        session.save(s);
    }

    /**
     * Find out if this user is associated with the group in an organisation
     * which is within the membership group
     *
     * @param groupName
     * @param org
     * @return
     */
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

    /**
     * Test if the current user is within a group, where the users membership
     * organisation is contained within the given parent organsiation
     *
     * @param groupName
     * @param parentOrg
     * @return
     */
    public boolean isInChildGroup(String groupName, Organisation parentOrg) {
        if (getMemberships() != null) {
            for (GroupMembership m : getMemberships()) {
                if (m.getGroupEntity().getName().equals(groupName)) {
                    if (m.getWithinOrg().isWithin(parentOrg)) {
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
        if (getRepositories() != null) {
            for (Repository r : getRepositories()) {
                if (r instanceof AddressBook) {
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
        if (getFirstName() != null && getFirstName().length() > 0) {
            name += getFirstName();
        }
        if (getSurName() != null && getSurName().length() > 0) {
            name = name + " " + getSurName();
        }
        return name;
    }
    
    public Calendar calendar(String name) {
        System.out.println("calendar: " + name);
        if( getCalendars() == null ) {
            System.out.println("null calendars");
            return null;
        }
        for(Calendar c : getCalendars()) {
            System.out.println("check cal: " + c.getName() + " --- " + name);
            if( c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public AddressBook addressBook(String name) {
        if( getAddressBooks() != null) {
            for( AddressBook a : getAddressBooks() ) {
                if( a.getName().equals(name)) {
                    return a;
                }
            }
                
        }
        return null;
    }
}
