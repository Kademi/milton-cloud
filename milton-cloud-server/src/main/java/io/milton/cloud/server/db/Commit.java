package io.milton.cloud.server.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * A Commit is a link between a Repository and an ItemVersion
 * 
 * The ItemVersion linked to is a directory, and its members are the 
 * members of the Repository for this version
 * 
 * The latest version for a Repository (ie with the highest versionNum)
 * is the current version of the repository (ie the Head)
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(name="COMMIT_ITEM")
public class Commit implements Serializable {
    private long id;
    private ItemVersion rootItemVersion; // this is the root directory for the repository (in this version)
    
    // parent
    private Branch branch;
    private Date createdDate; 
           
    /**
     * The user which created this version
     */
    private Profile editor;

    public Commit() {
    }
        
    @ManyToOne(optional=false)
    public ItemVersion getRootItemVersion() {
        return rootItemVersion;
    }

    public void setRootItemVersion(ItemVersion rootItemVersion) {
        this.rootItemVersion = rootItemVersion;
    }    
    
    @ManyToOne(optional=false)    
    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch repo) {
        this.branch = repo;
    }

    @ManyToOne
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
