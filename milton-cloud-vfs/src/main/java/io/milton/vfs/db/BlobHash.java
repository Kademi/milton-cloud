package io.milton.vfs.db;

import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

/**
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class BlobHash implements Serializable {
    private long blobHash;
    private long volumeId;
    
    public static BlobHash findByHash(long hash) {
        return (BlobHash) SessionManager.session().get(BlobHash.class, hash);
    }    
    

    @Id    
    @Index(name="ids_blobhash")
    public long getBlobHash() {
        return blobHash;
    }

    public void setBlobHash(long hash) {
        this.blobHash = hash;
    }

    @Column(nullable=false)
    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }
    
    
}
