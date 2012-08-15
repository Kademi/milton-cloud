package io.milton.vfs.db;

import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import java.util.ArrayList;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

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
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    @Index(name="ids_entity_name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public boolean containsUser(BaseEntity user, Organisation withinOrg) {
        return this == user;
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

    public void delete(Session session) {      
        
        if( getRepositories() != null ) {
            for( Repository r : getRepositories()) {
                r.delete(session);
            }
            setRepositories(null);
        }
        
        if( getMemberships() != null ) {
            for( GroupMembership m : getMemberships() ) {
                session.delete(m);
            }
            setMemberships(null);
        }
        if( getAddressBooks() != null ) {
            for( AddressBook a : getAddressBooks() ) {
                a.delete(session);
            }
            setAddressBooks(null);
        }
        if( getCalendars() != null ) {
            for( Calendar cal : getCalendars() ) {
                cal.delete(session); 
            }
            setCalendars(null);
        }
        session.delete(this);
    }
     
    
    /**
     * Create a GroupMembership linking this profile to the given group, within the given
     * organisation. Is immediately saved
     * 
     * @param g
     * @return 
     */
    public BaseEntity addToGroup(Group g, Organisation withinOrg) {
        if( g.isMember(this, withinOrg)) {
            return this;
        }
        GroupMembership gm = new GroupMembership();
        gm.setCreatedDate(new Date());
        gm.setGroupEntity(g);
        gm.setMember(this);
        gm.setWithinOrg(withinOrg);
        gm.setModifiedDate(new Date());
        SessionManager.session().save(gm);
        return this;
    }    
    
    public Repository repository(String name) {
        if( getRepositories() != null ) {
            for( Repository r : getRepositories() ) {
                if( r.getName().equals(name)) {
                    return r;
                }
            }
        }        
        return null;
    }
}
