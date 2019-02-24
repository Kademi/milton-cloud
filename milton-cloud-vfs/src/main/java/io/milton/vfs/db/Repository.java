package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.persistence.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
//        uniqueConstraints = {
//            @UniqueConstraint(columnNames = {"name", "base_entity"})}
)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("R")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Repository implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    public static void initRepo(Repository r, String name, Session session, Profile user, BaseEntity owner) throws HibernateException {
        r.setBaseEntity(owner);
        if (owner.getRepositories() == null) {
            owner.setRepositories(new ArrayList<Repository>());
        }
        owner.getRepositories().add(r);
        r.setCreatedDate(new Date());
        r.setName(name);
        r.setTitle(name);
        r.setLiveBranch(Branch.TRUNK);
        if (session != null) {
            try {
                session.save(r);
            } catch (Throwable e) {
                throw new RuntimeException("Exception saving repo", e);
            }
        }

        r.createBranch(Branch.TRUNK, user, session);
    }
    private long id;
    private String type;
    private String name; // identifies the resource to webdav
    private String title; // user friendly title
    private String notes;
    private String liveBranch;
    private boolean publicContent; // allow anonymous users to view this content
    private Date createdDate;
    private Boolean deleted;
    private BaseEntity baseEntity; // the direct owner of this repository
    private List<Branch> branches;

    public Repository() {
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * The entity that contains this repository. The entity might be a user,
     * organisation, etc
     *
     * The repository name must be unique within its parent entity
     *
     * @return
     */
    @ManyToOne(optional = false)
    public BaseEntity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(BaseEntity baseEntity) {
        this.baseEntity = baseEntity;
    }

    @Column(length = 255, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = 255)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * To support soft deletes
     *
     * @return
     */
    @Column
    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Column
    public String getLiveBranch() {
        return liveBranch;
    }

    public void setLiveBranch(String liveBranch) {
        this.liveBranch = liveBranch;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @OneToMany(mappedBy = "repository")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Branch> getBranches() {
        return branches;
    }

    public void setBranches(List<Branch> branches) {
        this.branches = branches;
    }

    @Column(insertable = false, updatable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(nullable = false)
    public boolean isPublicContent() {
        return publicContent;
    }

    public void setPublicContent(boolean publicContent) {
        this.publicContent = publicContent;
    }

    @Transient
    public Branch getTrunk() {
        return liveBranch();
    }

    /**
     * Creates and saves a new branch, including setting up initial commit etc
     *
     * @param name
     * @param user
     * @param session
     * @return
     */
    public Branch createBranch(String name, Profile user, Session session) {
        if (user == null) {
            throw new RuntimeException("Cant create branch with null user");
        }
        Commit head = new Commit();
        head.setCreatedDate(new Date());
        head.setEditor(user);
        head.setItemHash(null);
        if (session != null) {
            session.save(head);
        }

        Branch b = new Branch();
        b.setName(name);
        b.setRepository(this);
        b.setHead(head);
        if (session != null) {
            session.save(b);
        }

        head.setBranch(b);
        if (session != null) {
            session.save(b);
        }

        if (getBranches() == null) {
            setBranches(new ArrayList<Branch>());
        }
        getBranches().add(b);

        return b;
    }

    public void delete(Session session) {
        if (getBranches() != null) {
            Iterator<Branch> it = getBranches().iterator();
            while (it.hasNext()) {
                Branch b = it.next();
                b.delete(session);
                it.remove();
            }
        }
        session.flush();
        session.delete(this);
        session.flush();
    }

    /**
     * Get the discriminator type for this instance. Should just use getType,
     * but for some reason is returning null
     *
     * @return
     */
    public String type() {
        return "R";
    }

    public Branch branch(String branchName) {
        if (getBranches() == null) {
            return null;
        }
        for (Branch b : getBranches()) {
            if (b.getName().equals(branchName)) {
                return b;
            }
        }
        return null;
    }

    public Branch liveBranch() {
        if (getLiveBranch() != null) {
            Branch b = branch(getLiveBranch());
            if (b == null) {
                log.warn("Null branch in repo: " + getName());
                //Thread.dumpStack();
            }
            return b;
        } else {
            return null;
        }
    }

    public void softDelete(Session session) {
        this.setDeleted(true);
        String deletedName = Organisation.getDeletedName(getName()); // change name to avoid name conflicts with new resources
        this.setName(deletedName);
        session.save(this);
    }

    /**
     * null safe alias for getDeleted
     *
     * @return
     */
    public boolean deleted() {
        if (getDeleted() == null) {
            return false;
        } else {
            return getDeleted();
        }
    }

    /**
     * Overridden by subclasses to return a definite identifier for the type of
     * Repository, since we can't use instanceof with Hibernate classes
     *
     * @return
     */
    @Transient
    public String getRepoType() {
        return "R";
    }

    /**
     * Check that this website is contained by the given organisation. Ie that
     * the org directly owns this, or is a parent of the org that owns it
     *
     * @param entity
     * @return
     */
    public boolean isContainedBy(BaseEntity entity) {
        IsContainedVisitor v = new IsContainedVisitor(entity);
        getBaseEntity().accept(v);
        return v.isContained;
    }

    /**
     * Move this repository to the given owner
     *
     * @param dest
     * @param movedBy - the user performing the move
     * @param session
     */
    public void moveTo(BaseEntity dest, Profile movedBy, Session session) {
        getBaseEntity().getRepositories().remove(this);
        this.setBaseEntity(dest);
        dest.getRepositories().add(this);
        session.save(this);
        session.save(dest);
    }

    private class IsContainedVisitor extends AbstractVfsVisitor {

        final BaseEntity entity;
        boolean isContained;

        public IsContainedVisitor(BaseEntity entity) {
            this.entity = entity;
        }

        @Override
        public void visit(Organisation owner) {
            if (owner.getId() == entity.getId()) {
                isContained = true;
                return;
            }
            Organisation parent = owner.getOrganisation();
            if (parent != null) {
                parent.accept(this);
            }
        }

        @Override
        public void visit(Profile owner) {
            if (owner.getId() == entity.getId()) {
                isContained = true;
            }
        }
    }
}
