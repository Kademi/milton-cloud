package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
    private long id;
    private String itemHash; // this is the root directory for the repository (in this version)   
    private Date createdDate; 
    private Profile editor;

    public Commit() {
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
