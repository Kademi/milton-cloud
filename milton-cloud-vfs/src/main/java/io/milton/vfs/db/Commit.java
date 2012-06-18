package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

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
public class Commit implements Serializable {
    private long id;
    private long itemHash; // this is the root directory for the repository (in this version)   
    private Date createdDate; 
    private long editorId;

    public Commit() {
    }

    public long getItemHash() {
        return itemHash;
    }

    public void setItemHash(long itemHash) {
        this.itemHash = itemHash;
    }

    public long getEditorId() {
        return editorId;
    }

    public void setEditorId(long editorId) {
        this.editorId = editorId;
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
