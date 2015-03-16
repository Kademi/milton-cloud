package io.milton.vfs.db;

import io.milton.cloud.common.With;
import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.utils.SessionManager;
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

    public static final String DEFAULT_CALENDAR_NAME = "default";

    private List<AttendeeRequest> attendeeRequests;
    private static final Logger log = LoggerFactory.getLogger(Profile.class);
    public static final String ENTITY_TYPE_PROFILE = "U";

    public static Profile create(String email, Date now) {
        Profile p = new Profile();
        p.setEmail(email.toLowerCase());
        p.setCreatedDate(now);
        p.setModifiedDate(now);
        return p;
    }

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
     * @param parameters
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

    public static List<Profile> findByGroup(Group group, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        // join to group membership, then subordinate, then restrict on org        
        Criteria critMembership = crit.createCriteria("memberships");
        critMembership.add(Restrictions.eq("groupEntity", group));
        return DbUtils.toList(crit, Profile.class);
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

    /**
     * Find a user who has a membership in the given organisation
     *
     * @param org
     * @param name
     * @param session
     * @return
     */
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
        critMembership.add(Restrictions.eq("withinOrg", organisation));
        return DbUtils.toList(crit, Profile.class);
    }

    public static List<Profile> findAll(Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.setCacheable(true);
        return DbUtils.toList(crit, Profile.class);

    }

    /**
     * Find a profile by email address, but only looking within the given
     * organisation or subordinate orgs
     *
     * @param email
     * @param org
     * @param session
     * @return
     */
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
            // have 20 tries, then shove some random digits on the end
            if( counter > 20 ) {
                String suffix = (System.currentTimeMillis() + "");
                suffix = suffix.substring(suffix.length()-4);
                nickName = nickName + "-" + suffix;
            }
        }
        return candidateName;
    }

    public static boolean isUniqueName(String name, Session session) {
        log.info("isUniqueName");
        long tm = System.currentTimeMillis();
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Restrictions.eq("name", name));
        Object result = DbUtils.unique(crit);
        log.info("isUniqueName name={} result={} durationMs={}", name, result, (System.currentTimeMillis()-tm));
        return result == null;
    }
    
    private String name;
    private String firstName;
    private String surName;
    private String phone;
    private String email;
    private String photoHash;
    private String origPhotoHash;
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

    @OneToMany(mappedBy = "profile", fetch = FetchType.LAZY)
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
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
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

    /**
     * Hash of the reduced resolution avatar image
     *
     * @return
     */
    public String getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(String photoHash) {
        this.photoHash = photoHash;
    }

    /**
     * Hash of the original, full resolution, photo uploaded as the profile
     *
     * @return
     */
    public String getOrigPhotoHash() {
        return origPhotoHash;
    }

    public void setOrigPhotoHash(String origPhotoHash) {
        this.origPhotoHash = origPhotoHash;
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
     * Returns the first membership for the given group. Often users will only
     * have one membership for a group, but they are permitted to have multiple
     * memberships to a group in different orgs
     *
     * @param group
     * @return
     */
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

    public List<GroupMembership> memberships(Group group) {
        List<GroupMembership> list = new ArrayList<>();
        if (getMemberships() != null) {
            for (GroupMembership gm : getMemberships()) {
                if (gm.getGroupEntity() == group) {
                    list.add(gm);
                }
            }
        }
        return list;
    }

    public void removeMembership(Group group, Session session) {
        if (getMemberships() != null) {
            Iterator<GroupMembership> it = getMemberships().iterator();
            List<GroupMembership> toRemove = new ArrayList<>();
            while (it.hasNext()) {
                GroupMembership gm = it.next();
                if (gm.getGroupEntity() == group) {
                    toRemove.add(gm);
                }
            }
            if (!toRemove.isEmpty()) {
                for (GroupMembership gm : toRemove) {
                    gm.delete(session);
                }
                session.flush();
            }
        }
    }

    /**
     *
     * @param g
     * @param hasGroupInOrg - the member will be created for this organisation.
     * NOT always the same as the org which owns the group
     * @param session
     * @param membershipCreatedCallback - called only if a membership is
     * created. Optional. actually created
     * @return
     */
    public GroupMembership getOrCreateGroupMembership(Group g, Organisation hasGroupInOrg, Session session, With<GroupMembership, Object> membershipCreatedCallback) {
        GroupMembership gm = getGroupMembership(g, hasGroupInOrg, session);
        if (gm != null) {
            return gm;
        } else {
            gm = createGroupMembership(g, hasGroupInOrg, session);
            if (membershipCreatedCallback != null) {
                try {
                    membershipCreatedCallback.use(gm);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return gm;
        }
    }

    public GroupMembership getGroupMembership(Group g, Organisation hasGroupInOrg, Session session) {
        if (getMemberships() != null) {
            for (GroupMembership gm : getMemberships()) {
                if (gm.getGroupEntity().getId() == g.getId()) {
                    // same group
                    if (gm.getWithinOrg().getId() == hasGroupInOrg.getId()) {
                        // and same org, so its a duplicate
                        return gm;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create a GroupMembership linking this profile to the given group, within
     * the given organisation. Is immediately saved
     *
     * If a duplicate membership exists an exception is thrown
     *
     * Check for existing membership by calling getGroupMembership(group,
     * hasGruopInOrg, session)
     *
     * @param g
     * @param hasGroupInOrg
     * @param session
     * @return
     */
    public GroupMembership createGroupMembership(Group g, Organisation hasGroupInOrg, Session session) {
        GroupMembership gm = getGroupMembership(g, hasGroupInOrg, session);
        if (gm != null) {
            return gm;
        }
        gm = new GroupMembership();
        gm.setCreatedDate(new Date());
        gm.setGroupEntity(g);
        gm.setMember(this);
        gm.setWithinOrg(hasGroupInOrg);
        gm.setModifiedDate(new Date());
        session.save(gm);

        if (g.getGroupMemberships() == null) {
            g.setGroupMemberships(new ArrayList<GroupMembership>());
        }
        g.getGroupMemberships().add(gm);

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
        return gm;
    }

    /**
     * Creates a Subordinate record
     *
     * @param subordinateTo
     * @param gm
     */
    public void createSubordinate(Organisation subordinateTo, GroupMembership gm, Session session) {
        Subordinate s = new Subordinate();
        s.setWithinOrg(subordinateTo);
        s.setGroupMembership(gm);
        if (gm.getSubordinates() == null) {
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

    public boolean isInGroup(Group group) {
        List<GroupMembership> list = GroupMembership.find(group, this, SessionManager.session());
        return !list.isEmpty();
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
    @Override
    public String getFormattedName() {
        String fname = "";
        if (getFirstName() != null && getFirstName().length() > 0) {
            fname += getFirstName();
        }
        if (getSurName() != null && getSurName().length() > 0) {
            fname = fname + " " + getSurName();
        }
        if (fname.length() == 0) {
            if (getNickName() != null && getNickName().length() > 0) {
                fname = getNickName();
            }
        }
        if (fname.length() == 0) {
            fname = getName();
        }
        return fname;
    }

    @Transient
    @Override
    public String getEntityName() {
        return getName();
    }

    @Transient
    public Date getPasswordCredentialDate() {
        if (getCredentials() != null) {
            for (Credential c : getCredentials()) {
                if (c instanceof PasswordCredential) {
                    return c.getCreatedDate();
                }
            }
        }
        return null;
    }

    @OneToMany(mappedBy = "attendee")
    public List<AttendeeRequest> getAttendeeRequests() {
        return attendeeRequests;
    }

    public void setAttendeeRequests(List<AttendeeRequest> attendeeRequests) {
        this.attendeeRequests = attendeeRequests;
    }

    /**
     * Find all memberships of this user to organisations of the given type,
     * which are within the given parent org. If the orgtype is null returns all
     * memberships withint the given parent org
     *
     *
     * @param ot
     * @param parentOrg
     * @return
     */
    public List<GroupMembership> membershipsForOrgType(OrgType ot, Organisation parentOrg) {
        List<GroupMembership> list = new ArrayList<>();
        if (ot == null) {
            // use any membership from within the awarding org
            if (getMemberships() != null) {
                for (GroupMembership m : getMemberships()) {
                    if (m.getWithinOrg().isWithin(parentOrg)) {
                        list.add(m);
                    }
                }
            }
        } else {
            // find a membership to an org of type pointsOrgType
            if (getMemberships() != null) {
                for (GroupMembership m : getMemberships()) {
                    if (m.getWithinOrg().isWithin(parentOrg)) {
                        if (m.getWithinOrg().getOrgType() != null && ot.getId() == m.getWithinOrg().getOrgType().getId()) {
                            list.add(m);
                        }
                    }
                }
            }
        }
        return list;
    }

    public List<GroupMembership> memberships(Organisation parentOrg) {
        List<GroupMembership> list = new ArrayList<>();
        if (getMemberships() != null) {
            for (GroupMembership gm : getMemberships()) {
                if (gm.getGroupEntity().getOrganisation() == parentOrg) {
                    list.add(gm);
                }
            }
        }
        return list;
    }

    public Calendar defaultCalendar(Session session) {
        Calendar cal = calendar(DEFAULT_CALENDAR_NAME);
        if (cal == null) {
            if (getCalendars() != null && !getCalendars().isEmpty()) {
                return getCalendars().get(0);
            }
        }
        cal = newCalendar("default", this, session);
        return cal;
    }

    /**
     * Get the primary membership of this user within the given root
     * organsiation
     *
     * @param rootOrg
     * @return
     */
    public GroupMembership primaryMembership(Organisation rootOrg) {
        for (GroupMembership gm : memberships(rootOrg)) {
            if (gm.getGroupEntity().isPrimary()) {
                return gm;
            }
        }
        return null;
    }

}
