package io.milton.vfs.db;

import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 *
 * @author brad
 */
@Entity
public class BlobHash implements Serializable {
    private long blobHash;
    private long volumeId;
    
    public static BlobHash findByHash(long hash) {
        return (BlobHash) SessionManager.session().get(BlobHash.class, hash);
    }    
    

    @Id    
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
