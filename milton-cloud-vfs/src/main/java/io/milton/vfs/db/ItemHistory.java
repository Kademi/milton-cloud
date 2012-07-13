package io.milton.vfs.db;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This class captures the state of a resource and its location within a hierarchy
 * immediately after a change has occured. It keeps a reference to its name at that
 * time and the parent, as well as the action which caused the change
 * 
 * To find prior state you must list these records for the related MetaItem
 *
 * @author brad
 */
@javax.persistence.Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ItemHistory implements Serializable {

    private long id;
    
    private UUID treeItem; // the treeitem meta ID this version relates to
    
    private String name; // the name at the time of this version
    private UUID parentTreeItem; // the parent at the time of the version
    private String action; // what happened? u=update/create,d=delete,m=move

    /**
     * The tree item this version relates to
     * 
     * @return 
     */
    @Column(nullable=false)
    public UUID getTreeItem() {
        return treeItem;
    }

    public void setTreeItem(UUID treeItem) {
        this.treeItem = treeItem;
    }

    
    
    /**
     * @return the name
     */
    @Column(length = 1000)
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The parent at the time of the update
     * 
     * @return the hash
     */
    @Column(nullable=false)
    public UUID getParentTreeItem() {
        return parentTreeItem;
    }

    public void setParentTreeItem(UUID parentTreeItem) {
        this.parentTreeItem = parentTreeItem;
    }


    

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
    
    

}
