package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Commit is a link between a Repository and an TreeItem
 *
 * The TreeItem linked to is a directory, and its members are the members of the
 * Repository for this version
 *
 * The latest version for a Repository (ie with the highest versionNum) is the
 * current version of the repository (ie the Head)
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name = "BRANCH", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"repository", "name"})}// unique names within a repository
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Branch implements Serializable, VfsAcceptor {

    /**
     * Special branch which always exists on a repository
     */
    public static String TRUNK = "version1";

    public static Branch get(Long branchId, Session session) {
        return (Branch)session.get(Branch.class, branchId);
    }
    
    private long id;
    private String name;
    private Long version;
    private Commit head;
    private Commit fromCommit;
    private Repository repository;
    private Date createdDate;
    private String publicTheme;
    private Profile hiddenBy;

    public Branch() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * If not null the branch is considered hidden, and the hiddenBy profile
     * indicates the user who hid it.
     * 
     * If null then the branch is not hidden
     * 
     * @return 
     */
    @ManyToOne
    public Profile getHiddenBy() {
        return hiddenBy;
    }

    public void setHiddenBy(Profile hiddenBy) {
        this.hiddenBy = hiddenBy;
    }

    
    
    /**
     * The public theme is intended for non-logged in access. It will usually
     * control the landing page and other content pages available to users prior
     * to signing up or logging in
     *
     * @return
     */
    @Column
    public String getPublicTheme() {
        return publicTheme;
    }

    public void setPublicTheme(String publicTheme) {
        this.publicTheme = publicTheme;
    }

    @Version
    @Column(name = "LOCKVERSION")
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @ManyToOne(optional = false)
    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repo) {
        this.repository = repo;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public Commit getHead() {
        return head;
    }

    public void setHead(Commit head) {
        this.head = head;
    }

    /**
     * Get the commit this branch was created from, if any
     *
     * @return
     */
    @ManyToOne(optional = true)
    public Commit getFromCommit() {
        return fromCommit;
    }

    public void setFromCommit(Commit from) {
        this.fromCommit = from;
    }

    public Commit latestVersion(Session session) {
        return head;
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public void delete(Session session) {
        session.delete(this);
    }

    public Branch copy(String newName, Date now, Session session) {
        return copy(repository, newName, now, session);
    }

    /**
     * Creates and saves a copy of this branch with the new name
     *
     * @param repo
     * @param newName
     * @param now
     * @param session
     * @return
     */
    public Branch copy(Repository repo, String newName, Date now, Session session) {
        Branch b = new Branch();
        b.setFromCommit(this.getHead());
        b.setCreatedDate(now);
        b.setName(newName);
        b.setPublicTheme(getPublicTheme());
        b.setRepository(repo);
        b.setHead(head);
        session.save(b);

        if (repo.getBranches() == null) {
            repo.setBranches(new ArrayList<>());
        }
        repo.getBranches().add(b);

        return b;
    }
}
