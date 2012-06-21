package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;

/**
 * A repository
 *
 * @author brad
 */
@javax.persistence.Entity
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "base_entity"})}
)

@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("R")
@Inheritance(strategy = InheritanceType.JOINED)
public class Repository implements Serializable {

    private long id;
    private String name; // identifies the resource to webdav
    private String title; // user friendly title
    private String notes;
    private Date createdDate;
    private BaseEntity baseEntity; // the direct owner of this repository
    private List<Branch> branches;
    private List<NvPair> nvPairs; // holds data capture information

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public BaseEntity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(BaseEntity baseEntity) {
        this.baseEntity = baseEntity;
    }

    
    
    @Column(length = 255, nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length=255)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }    
    
    @Temporal(javax.persistence.TemporalType.DATE)
    @Column(nullable = false)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @OneToMany(mappedBy = "repository")
    public List<Branch> getBranches() {
        return branches;
    }

    public void setBranches(List<Branch> branches) {
        this.branches = branches;
    }
    
    @OneToMany(mappedBy = "repository")
    public List<NvPair> getNvPairs() {
        return nvPairs;
    }

    public void setNvPairs(List<NvPair> nvPairs) {
        this.nvPairs = nvPairs;
    }    

    public Repository setAttribute(String name, String value, Session session) {
        List<NvPair> list = getNvPairs();
        if( list == null ) {
            list = new ArrayList<>();
            setNvPairs(list);
        }
        for( NvPair nv : list ) {
            if( nv.getName().equals(name)) {
                nv.setPropValue(value);
                session.save(nv);
            }
        }
        NvPair nv = new NvPair();
        nv.setRepository(this);
        nv.setName(name);
        nv.setPropValue(value);
        list.add(nv);
        session.save(nv);
        return this;
    }
    
    public String getAttribute(String name) {
        List<NvPair> list = getNvPairs();
        if( list == null ) {
            return null;
        }
        for( NvPair nv : list ) {
            if( nv.getName().equals(name)) {
                return nv.getPropValue();
            }
        }
        return null;
    }    
    
    public Branch trunk(Session session) {
        if (getBranches() != null) {
            for (Branch b : getBranches()) {
                if (Branch.TRUNK.equals(b.getName())) { 
                    return b;
                }
            }
        }
        return null;
    }  
    
    /**
     * Creates and saves a new branch, including setting up initial commit etc
     * 
     * @param name
     * @param user
     * @param session
     * @return 
     */
    public Branch createBranch(String name, Profile user, Session session) {
        Commit head = new Commit();
        head.setCreatedDate(new Date());
        head.setEditor(user);
        head.setItemHash(0);
        session.save(head);
        
        MetaItem rootMetaItem = new MetaItem();
        rootMetaItem.setCreatedDate(new Date());
        rootMetaItem.setModifiedDate(new Date());
        session.save(rootMetaItem);
        
        Branch b = new Branch();
        b.setName(Branch.TRUNK);
        b.setRepository(this);
        b.setHead(head);
        b.setRootMetaItem(rootMetaItem);
        session.save(b);    
        
        if( getBranches() == null ) {
            setBranches(new ArrayList<Branch>());
        }
        getBranches().add(b);
        
        return b;
    }
}
