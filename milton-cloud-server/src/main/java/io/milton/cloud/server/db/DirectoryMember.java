package io.milton.cloud.server.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.Session;

/**
 * A DirectoryMember represents the existence of an item within a particular
 * directory
 *
 * The list of DirectoryMember objects within a directory defines that directory
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "parent_item"})}// item names must be unique within a directory
)
public class DirectoryMember implements Serializable {

    private long id;
    private String name;
    private ItemVersion parentItem;
    private ItemVersion memberItem; // this is the hash of this item

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
     * @return the hash
     */
    @ManyToOne(optional = false)
    public ItemVersion getParentItem() {
        return parentItem;
    }

    /**
     * @param hash the hash to set
     */
    public void setParentItem(ItemVersion parentItem) {
        this.parentItem = parentItem;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public ItemVersion getMemberItem() {
        return memberItem;
    }

    public void setMemberItem(ItemVersion memberItem) {
        this.memberItem = memberItem;
    }

    /**
     * Create a new version of this directory members parent directory,
     * including new instances of its members. Other recreated dir members will
     * still have the same version, but this one will be linked to the given IV
     *
     * @param newMemberIV
     */
    public void updateTo(ItemVersion newMemberIV, Session session) {
        ItemVersion newParentIV = new ItemVersion();
        newParentIV.setItem(getParentItem().getItem());
        newParentIV.setModifiedDate(new Date());
        newParentIV.setLinked(new ArrayList<DirectoryMember>());
        newParentIV.setMembers(new ArrayList<DirectoryMember>());
        if (this.getParentItem().getMembers() != null) {
            for (DirectoryMember siblingDm : this.getParentItem().getMembers()) {
                DirectoryMember newDm = new DirectoryMember();
                newDm.setName(siblingDm.getName());
                newDm.setParentItem(newParentIV);
                newParentIV.getMembers().add(newDm);
                if (siblingDm != this) {
                    newDm.setMemberItem(siblingDm.getMemberItem());
                } else {
                    newDm.setMemberItem(newMemberIV);
                }
            }
        }
        newParentIV.calcHash();
        session.save(newParentIV);

        // now recurse up the inverted tree
        if (this.getParentItem().getLinked() != null) {
            for (DirectoryMember parentDm : this.getParentItem().getLinked()) {
                parentDm.updateTo(newParentIV, session);
            }
        }
    }
}
