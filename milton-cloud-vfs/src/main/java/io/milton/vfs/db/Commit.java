package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

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
@Table(name="COMMIT_ITEM")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Commit implements Serializable {

    public static Commit find(Repository repo, long commitId, Session session) {
        Commit c = (Commit) session.get(Commit.class, commitId);
        if( c == null ) {
            return null;
        }
        if( c.getBranch().getRepository() != repo ) {
            return null;
        }
        return c;
    }
    
    public static List<Commit> findByBranch(Branch branch, Session session) {
        Criteria c = session.createCriteria(Commit.class);
        c.add(Restrictions.eq("branch", branch));
        c.addOrder(Order.desc("createdDate"));
        return DbUtils.toList(c, Commit.class);
    }
    
    private long id;
    private Branch branch;
    private Long previousCommitId;
    private String itemHash; // this is the root directory for the repository (in this version)   
    private Date createdDate; 
    private Profile editor;

    public Commit() {
    }

    @ManyToOne
    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    /**
     * This was originally a reference to the object, but it caused a StackOverflowError
     * in some cases. Was impossible to determine exact cause but changing to
     * an id instead of reference for safety.
     * 
     * @return 
     */
    public Long getPreviousCommitId() {
        return previousCommitId;
    }

    public void setPreviousCommitId(Long previousCommitId) {
        this.previousCommitId = previousCommitId;
    }

    
    

    
    
    @Column
    public String getItemHash() {
        return itemHash;
    }

    public void setItemHash(String itemHash) {
        this.itemHash = itemHash;
    }

    /**
     * The user who created this commit
     * 
     * @return 
     */
    @ManyToOne(optional=false)
    public Profile getEditor() {
        return editor;
    }

    public void setEditor(Profile editor) {
        this.editor = editor;
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

}
