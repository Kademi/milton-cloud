package io.milton.vfs.db;

import io.milton.cloud.common.ITriplet;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;

/**
 *
 *
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "parentHash"})// item names must be unique within a directory
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DataItem implements Serializable, ITriplet {

    public static  List<DataItem> findByHash(long hash, Session session) {
        Criteria crit = session.createCriteria(DataItem.class);
        crit.add(Expression.eq("parentHash", hash));
        return DbUtils.toList(crit, DataItem.class);
    }        
    
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

    /**
     * Required to implement ITriplet, this is not a persisted property, but
     * just returns getItemHash()
     */
    @Override
    @Transient
    public long getHash() {
        return itemHash;
    }
    
    
}
