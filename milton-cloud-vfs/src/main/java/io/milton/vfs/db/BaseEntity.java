package io.milton.vfs.db;

import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import java.util.ArrayList;

/**
 * Represents a real world entity such as a user or an organisation
 *
 * Any type of Entity can contain Repository objects.
 *
 * An entity can be both a recipient of permissions and a target of permissions.
 * For example a user entity can be given access to an organisation entity
 *
 * An entity name must be unique within the organisation that defines it. What
 * this name means depends on the context. For a user, the name is almost
 * meaningless
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name = "BASE_ENTITY",
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "organisation"})}// item names must be unique within a directory
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("E")
public abstract class BaseEntity implements Serializable, VfsAcceptor {

    public static BaseEntity find(Organisation org, String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Expression.and(Expression.eq("organisation", org), Expression.eq("name", name)));
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return (BaseEntity) list.get(0);
        }
    }

    public static List<BaseEntity> find(Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.add(Expression.eq("organisation", organisation));
        return DbUtils.toList(crit, BaseEntity.class);
    }
    
    
    private long id;
    private String name;
    private String type;
    private String notes;
    private Organisation organisation;
    private Date createdDate;
    private Date modifiedDate;
    private List<Permission> permissions; // permissions granted ON this entity
    private List<Permission> grantedPermissions; // permissions granted TO this entity
    private List<Repository> repositories;    // has repositories
    private List<GroupMembership> memberships; // can belong to groups
    private List<AddressBook> addressBooks; // has addressbooks
    private List<Calendar> calendars; // has calendars

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "grantee")
    public List<Permission> getGrantedPermissions() {
        return grantedPermissions;
    }

    public void setGrantedPermissions(List<Permission> grantedPermissions) {
        this.grantedPermissions = grantedPermissions;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "grantedOnEntity")
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
    
    

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "baseEntity")
    public List<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * Return true if this entity contains or is the given user
     *
     * @param user
     * @return
     */
    public boolean containsUser(BaseEntity user) {
        return user.getName().equals(this.getName()); // very simple because currenly only have users
    }

    @OneToMany(mappedBy = "owner")
    public List<Calendar> getCalendars() {
        return calendars;
    }

    public void setCalendars(List<Calendar> calendars) {
        this.calendars = calendars;
    }

    @OneToMany(mappedBy = "owner")
    public List<AddressBook> getAddressBooks() {
        return addressBooks;
    }

    public void setAddressBooks(List<AddressBook> addressBooks) {
        this.addressBooks = addressBooks;
    }

    @Column
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the groups that this entity is a member of. Not to be confused with
     * members on a group, which lists the entities which are in the group
     * 
     * @return 
     */
    @OneToMany(mappedBy = "member")
    public List<GroupMembership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<GroupMembership> memberships) {
        this.memberships = memberships;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    

    /**
     * Give this entity the given priviledge to access the grantedOn entity or resource
     * 
     * Usually "this" is a user or group, and grantedOn is a resource like an organisation
     *
     * @param priviledge
     * @param grantedOn
     */
    public void grant(AccessControlledResource.Priviledge priviledge, BaseEntity grantedOn, Session session) {
        if (isGranted(priviledge, grantedOn, session)) {
            return;
        }
        Permission p = new Permission();
        p.setGrantedOnEntity(grantedOn);
        p.setGrantee(this);
        p.setPriviledge(priviledge);
        session.save(p);
    }

    public boolean isGranted(AccessControlledResource.Priviledge priviledge, BaseEntity grantedOn, Session session) {
        Criteria crit = session.createCriteria(Permission.class);
        
        crit.add(
                Expression.and(Expression.eq("grantee", this), Expression.and(Expression.eq("grantedOnEntity", grantedOn), Expression.eq("priviledge", priviledge))));
        List list = crit.list();
        return list != null && !list.isEmpty();
    }
    
    public Repository createRepository(String name, Profile user, Session session) {
        if( getRepositories() == null ) {
            setRepositories(new ArrayList<Repository>());
        }
        Repository r = new Repository();
        r.setBaseEntity(this);
        getRepositories().add(r);
        r.setCreatedDate(new Date());
        r.setName(name);
        r.setTitle(name);
        session.save(r);
        
        r.createBranch(Branch.TRUNK, user, session);
        
        return r;
    }
      
}
