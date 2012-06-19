package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 * A MetaItem represents a resource within a specific folder in the Head commit
 * of a repository
 *
 * It holds metadata about the resource, such as modified date and created date
 *
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "parent"})}// item names must be unique within a directory
)
public class MetaItem implements Serializable {

    public static MetaItem find(MetaItem parent, String name, Session session) {
        Criteria crit = session.createCriteria(MetaItem.class);
        crit.add(
                Expression.and(
                Expression.eq("parent", parent),
                Expression.eq("name", name)));
        return DbUtils.unique(crit);
    }
    private long id;
    private MetaItem parent;
    private MetaItem deletedFromParent;
    private MetaItem linkedTo;
    private String name;
    private Date modifiedDate;
    private Date createdDate;
    private List<ItemHistory> versions;

    public MetaItem() {
    }

    public MetaItem getDirectChild(String name, Session session) {
        return find(this, name, session);
    }
    
    /**
     * Parent is null for resources which are the root of the repository
     *
     * @return
     */
    @ManyToOne(optional = true)
    public MetaItem getParent() {
        return parent;
    }

    public void setParent(MetaItem item) {
        this.parent = item;
    }

    @ManyToOne(optional = true)
    public MetaItem getDeletedFromParent() {
        return deletedFromParent;
    }

    public void setDeletedFromParent(MetaItem deletedFromParent) {
        this.deletedFromParent = deletedFromParent;
    }
    
    

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    @ManyToOne
    public MetaItem getRelatedTo() {
        return linkedTo;
    }

    public void setRelatedTo(MetaItem relatedTo) {
        this.linkedTo = relatedTo;
    }

    @Temporal(javax.persistence.TemporalType.DATE)
    @Column(nullable = false)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Temporal(javax.persistence.TemporalType.DATE)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @OneToMany(mappedBy = "treeItem")
    public List<ItemHistory> getVersions() {
        return versions;
    }

    public void setVersions(List<ItemHistory> versions) {
        this.versions = versions;
    }
    /**
     * Calculate the hash for this item, if it is a directory, and set it
     */
//    public void calcHash() {
//        if (getItem().getType().equals("d")) {
//            long newHash = HashCalc.calcHash(getMembers());
//            this.setItemHash(newHash);
//        }
//    }
}
