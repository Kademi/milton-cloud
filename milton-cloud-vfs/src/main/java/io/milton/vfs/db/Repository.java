package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A repository
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "base_entity"})})
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("R")
@Inheritance(strategy = InheritanceType.JOINED)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Repository implements Serializable {

    private long id;
    private String type;
    private String name; // identifies the resource to webdav
    private String title; // user friendly title
    private String notes;
    private String liveBranch;
    private boolean publicContent; // allow anonymous users to view this content
    private Date createdDate;
    private BaseEntity baseEntity; // the direct owner of this repository
    private List<Branch> branches;
    private List<NvPair> nvPairs; // holds data capture information

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

    @OneToMany(mappedBy = "repository")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<NvPair> getNvPairs() {
        return nvPairs;
    }

    public void setNvPairs(List<NvPair> nvPairs) {
        this.nvPairs = nvPairs;
    }

    @Column
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

    public Repository setAttribute(String name, String value, Session session) {
        if (value != null) {
            value = value.trim();
            if (value.length() == 0) {
                value = null;
            }
        }
        List<NvPair> list = getNvPairs();
        if (list == null) {
            list = new ArrayList<>();
            setNvPairs(list);
        }
        if (value == null) {
            Iterator<NvPair> it = list.iterator();
            while (it.hasNext()) {
                NvPair nv = it.next();
                if (nv.getName().equals(name)) {
                    session.delete(nv);
                    it.remove();
                }
            }
        } else {
            NvPair found = null;
            for (NvPair nv : list) {
                if (nv.getName().equals(name)) {
                    found = nv;
                    break;
                }
            }
            if (found == null) {
                found = new NvPair();
                found.setRepository(this);
                found.setName(name);
                list.add(found);
            }
            found.setPropValue(value);
            session.save(found);
        }
        return this;
    }

    public String getAttribute(String name) {
        List<NvPair> list = getNvPairs();
        if (list == null) {
            return null;
        }
        for (NvPair nv : list) {
            if (nv.getName().equals(name)) {
                return nv.getPropValue();
            }
        }
        return null;
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
        Commit head = new Commit();
        head.setCreatedDate(new Date());
        head.setEditor(user);
        head.setItemHash(null);
        session.save(head);

        Branch b = new Branch();
        b.setName(name);
        b.setRepository(this);
        b.setHead(head);
        session.save(b);

        if (getBranches() == null) {
            setBranches(new ArrayList<Branch>());
        }
        getBranches().add(b);

        return b;
    }

    public void delete(Session session) {
        if (getBranches() != null) {
            for (Branch b : getBranches()) {
                b.delete(session);
            }
            setBranches(null);
        }
        if (getNvPairs() != null) {
            for (NvPair p : getNvPairs()) {
                p.delete(session);
            }
            setNvPairs(null);
        }
        session.delete(this);
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
            return branch(getLiveBranch());
        } else {
            return null;
        }
    }
}
