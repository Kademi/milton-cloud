package io.milton.vfs.db;

import io.milton.resource.AccessControlledResource;
import io.milton.vfs.db.Permission.DynamicPrincipal;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;

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
@Cache(usage = CacheConcurrencyStrategy.NONE) // no cache until we get the file sync problem sorted out
public class Branch implements Serializable, VfsAcceptor {

    /**
     * Special branch which always exists on a repository
     */
    public static String TRUNK = "trunk";
    private long id;
    private String name;
    private Long version;
    private Commit head;
    private Repository repository;
    private Date createdDate;
    private List<Permission> permissions; // can be granted permissions

    public Branch() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    /**
     * Permissions which have been granted on this Branch
     *
     * @return
     */
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "grantedOnBranch")
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> grantedPermissions) {
        this.permissions = grantedPermissions;
    }

    public void grant(AccessControlledResource.Priviledge priviledge, DynamicPrincipal grantee) {
        if (isGranted(priviledge, grantee)) {
            return;
        }
        Permission p = new Permission();
        p.setGrantedOnBranch(this);
        p.setGranteePrincipal(grantee.name());
        p.setPriviledge(priviledge);
        SessionManager.session().save(p);
    }

    public boolean isGranted(AccessControlledResource.Priviledge priviledge, DynamicPrincipal grantee) {
        Session session = SessionManager.session();
        Criteria crit = session.createCriteria(Permission.class);
        crit.add(
                Expression.and(Expression.eq("granteePrincipal", grantee.name()), Expression.and(Expression.eq("grantedOnBranch", this), Expression.eq("priviledge", priviledge))));
        List list = crit.list();
        return list != null && !list.isEmpty();
    }
    
    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public void delete(Session session) {
        if( getPermissions() != null ) {
            for( Permission p : getPermissions() ) {
                session.delete(p);
            }
        }
        session.delete(this);
    }
}
