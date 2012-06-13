package io.milton.cloud.server.db;

import io.milton.http.AccessControlledResource.Priviledge;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import org.hibernate.Session;

/**
 * Represents a share of a Repository to another user or organisation
 *
 * @author brad
 */
@Entity
public class Share implements Serializable {

    public static Share get(UUID id, Session session) {
        return (Share) session.get(Share.class, id);
    }

    private UUID id;
    
    private Branch sharedFrom;
        
    private String shareRecip; // who it was sent to
    private Date createdDate;
    private Date acceptedDate;
    private Priviledge priv;

    /**
     * Use a random UUID so its cryptographically secure. This allows the identifier
     * to be used as a credential
     *
     * @return
     */
    @Id
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


    @ManyToOne(optional=false)
    public Branch getSharedFrom() {
        return sharedFrom;
    }

    public void setSharedFrom(Branch sharedFrom) {
        this.sharedFrom = sharedFrom;
    }

    
    
    /**
     * For situations where the sharedTo item is set some time after
     * the folder is shared (such as when sending a share over email)
     * this field holds the priviledge to be granted when the share is
     * accepted. It is then used to create the Permission object which will
     * allow access.
     * 
     * Note that this value has only historical meaning after the share has been accepted
     * 
     * @return 
     */
    public Priviledge getPriviledge() {
        return priv;
    }

    public void setPriviledge(Priviledge granted) {
        this.priv = granted;
    }

    /**
     * Optional, to record who this has been shared with. Such as an email
     * address
     * 
     * @return 
     */
    @Column
    public String getShareRecip() {
        return shareRecip;
    }

    public void setShareRecip(String shareRecip) {
        this.shareRecip = shareRecip;
    }

    @Temporal(javax.persistence.TemporalType.DATE)
    public Date getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(Date acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.DATE)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    
    
}
