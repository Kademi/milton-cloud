package io.milton.vfs.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * For holding permissions, etc
 *
 * Note included in hash of directory entries
 *
 *
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "parentHash"})// item names must be unique within a directory
})
public class DataItem implements Serializable {

    private long id;
    private String type; // "f" = file, "d" = directory
    private String name;
    private long itemHash;
    private long parentHash;

    public DataItem() {
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false, length = 1)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
     * Identifies the repository version number in which this change was made
     *
     * @return
     */
    @Column(nullable = false)
    public long getItemHash() {
        return itemHash;
    }

    public void setItemHash(long itemHash) {
        this.itemHash = itemHash;
    }

    /**
     * This is the hash value of the directory which contains this item. It is
     * the hash of the hashes, names and types of the DataItem's inside it
     * 
     * @return 
     */
    @Column(nullable = false)
    public long getParentHash() {
        return parentHash;
    }

    public void setParentHash(long parentHash) {
        this.parentHash = parentHash;
    }
    
    
}
