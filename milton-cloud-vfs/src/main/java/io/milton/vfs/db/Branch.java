package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;

/**
 * A Commit is a link between a Repository and an TreeItem
 * 
 * The TreeItem linked to is a directory, and its members are the 
 * members of the Repository for this version
 * 
 * The latest version for a Repository (ie with the highest versionNum)
 * is the current version of the repository (ie the Head)
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name="BRANCH")
public class Branch implements Serializable {
    
    /**
     * Special branch which always exists on a repository
     */
    public static String TRUNK = "trunk";    
    
    private long id;
    private String name;
    private Commit head;
    private MetaItem rootMetaItem;
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

    @ManyToOne(optional=false)
    public MetaItem getRootMetaItem() {
        return rootMetaItem;
    }

    public void setRootMetaItem(MetaItem rootMetaItem) {
        this.rootMetaItem = rootMetaItem;
    }
    
    
      
    @ManyToOne(optional=false)    
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

    @ManyToOne(optional=false)
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "grantedOnBranch")
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> grantedPermissions) {
        this.permissions = grantedPermissions;
    }        
}
