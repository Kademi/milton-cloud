package io.milton.vfs.db;

import java.io.Serializable;
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
@Table(name = "BRANCH")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Branch implements Serializable, VfsAcceptor {

    /**
     * Special branch which always exists on a repository
     */
    public static String TRUNK = "version1";
    private long id;
    private String name;
    private Long version;
    private Commit head;
    private Repository repository;
    private Date createdDate;
    private String internalTheme;
    private String publicTheme;    
    

    public Branch() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * The internal theme is intended for logged in access
     *
     * @return
     */
    @Column
    public String getInternalTheme() {
        return internalTheme;
    }

    public void setInternalTheme(String internalTheme) {
        this.internalTheme = internalTheme;
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
    @Column(name="LOCKVERSION")
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


}
