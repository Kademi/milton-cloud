package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import java.util.ArrayList;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

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
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("E")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class BaseEntity implements Serializable, VfsAcceptor {

    public static BaseEntity find(String name, Session session) {
        Criteria crit = session.createCriteria(BaseEntity.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("name", name));
        return DbUtils.unique(crit);
    }
    private long id;
    private String type;
    private String notes;
    private Date createdDate;
    private Date modifiedDate;
    private List<Repository> repositories;    // has repositories

    @Transient
    public abstract String getFormattedName();

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "baseEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

    @Column
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Repository createRepository(String name, Profile user, Session session) {
        if (user == null) {
            throw new RuntimeException("Cant create repository with a null user");
        }
        Repository r = new Repository();
        Repository.initRepo(r, name, session, user, this);
        return r;
    }

    public void delete(Session session) {

        if (getRepositories() != null) {
            for (Repository r : getRepositories()) {
                r.delete(session);
            }
            setRepositories(null);
        }

        session.delete(this);
    }

    /**
     * Returns all non-soft deleted repositories. Does not return null if empty
     *
     * @return
     */
    public List<Repository> repositories() {
        List<Repository> list = new ArrayList<>();
        if (getRepositories() != null) {
            for (Repository r : getRepositories()) {
                if (!r.deleted()) {
                    list.add(r);
                }
            }
        }
        return list;
    }

    public Repository repository(String name) {
        if (getRepositories() != null) {
            for (Repository r : getRepositories()) {
                if (r.getName().equals(name)) {
                    return r;
                }
            }
        }
        return null;
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
    public List<Calendar> getCalendars() {
        List<Calendar> list = new ArrayList();
        if (getRepositories() != null) {
            for (Repository r : getRepositories()) {
                if (r instanceof Calendar) {
                    Calendar ab = (Calendar) r;
                    list.add(ab);
                }
            }
        }
        return list;
    }
    

    public Calendar calendar(String name) {
        for (Calendar a : getCalendars()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public AddressBook addressBook(String name) {
        for (AddressBook a : getAddressBooks()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }

        return null;
    }    
}
