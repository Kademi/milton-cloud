package io.milton.vfs.db;

import java.io.Serializable;
import javax.persistence.*;

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
public class ItemHistory implements Serializable {

    private long id;
    
    private MetaItem treeItem; // the treeitem this version relates to
    
    private String name; // the name at the time of this version
    private MetaItem parentTreeItem; // the parent at the time of the version
    private String action; // what happened? u=update/create,d=delete,m=move

    /**
     * The tree item this version relates to
     * 
     * @return 
     */
    @ManyToOne(optional=false)
    public MetaItem getTreeItem() {
        return treeItem;
    }

    public void setTreeItem(MetaItem treeItem) {
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
    @ManyToOne(optional = false)
    public MetaItem getParentTreeItem() {
        return parentTreeItem;
    }

    public void setParentTreeItem(MetaItem parentTreeItem) {
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
