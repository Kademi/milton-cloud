package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Expression;

/**
 * A user profile is defined within an organisation. Might change this in the future so
 * that the user profile is within an organsiation, but the credentials probably should exist
 * in a global space.
 *
 * @author brad
 */
@javax.persistence.Entity
@DiscriminatorValue("U")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Profile extends BaseEntity implements VfsAcceptor {
    
    
    public static List<Profile> findByBusinessUnit(Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.add(Expression.eq("adminOrg", organisation));        
        return DbUtils.toList(crit, Profile.class);        
    }
    
     
    private Organisation businessUnit; // users may be assigned to a business unit within their administrative organisation
    
    private List<Credential> credentials;
                
    private String firstName;
    
    private String surName;
    
    private String phone;

    private String email;
    
    private long photoHash;
    
    private String nickName;
    
    private boolean enabled;
    
    private boolean rejected;

    @OneToMany(mappedBy="profile")
    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }
               
    public void setEmail(String email) {
        this.email = email;
    }

    @Column
    @Index(name="ids_profile_email")
    public String getEmail() {
        return email;
    }
    
        
    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column
    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(long photoHash) {
        this.photoHash = photoHash;
    }

    
    
    

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Users may be assigned to a business unit within their administative organisation
     * 
     */
    @ManyToOne(optional=true)
    public Organisation getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(Organisation businessUnit) {
        this.businessUnit = businessUnit;
    }

    /**
     * True means the user can login
     * 
     * @return 
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
      
    
    
    
    /**
     * Create a GroupMembership linking this profile to the given group. Is immediately saved
     * 
     * @param g
     * @return 
     */
    public Profile addToGroup(Group g) {
        if( g.isMember(this)) {
            return this;
        }
        GroupMembership gm = new GroupMembership();
        gm.setCreatedDate(new Date());
        gm.setGroupEntity(g);
        gm.setMember(this);
        gm.setModifiedDate(new Date());
        SessionManager.session().save(gm);
        return this;
    }

    @Override
    public void accept(VfsVisitor visitor) {
        visitor.visit(this);
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
    
    /**
     * True if the user belongs to a group with the Administrator GroupRole
     * 
     * @return 
     */
    @Transient
    public boolean isAdmin() {
        if( getMemberships() == null ) {
            return false;
        }
        for( GroupMembership m : getMemberships() ) {
            if( m.getGroupEntity().hasRole(GroupRole.ROLE_ADMIN)) {
                return true;
            }
        }
        return false;
    }
    
}
