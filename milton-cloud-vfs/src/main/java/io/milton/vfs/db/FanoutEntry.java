package io.milton.vfs.db;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

/**
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FanoutEntry implements Serializable{
    private long id;
    private FanoutHash fanout;
    private long chunkHash;

    @Column
    @Index(name="ids_chunk_hash")
    public long getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(long chunkHash) {
        this.chunkHash = chunkHash;
    }

    @ManyToOne
    public FanoutHash getFanout() {
        return fanout;
    }

    public void setFanout(FanoutHash fanout) {
        this.fanout = fanout;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    
    
}
