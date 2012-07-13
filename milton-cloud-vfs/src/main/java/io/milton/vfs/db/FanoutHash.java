package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.hashsplit4j.api.Fanout;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FanoutHash implements Serializable, Fanout{
    private long hash;
    private long actualContentLength;
    private List<FanoutEntry> fanoutEntrys;

    @Id
    public long getFanoutHash() {
        return hash;
    }

    public void setFanoutHash(long hash) {
        this.hash = hash;
    }

    @Column(nullable=false)
    @Override
    public long getActualContentLength() {
        return actualContentLength;
    }

    public void setActualContentLength(long actualContentLength) {
        this.actualContentLength = actualContentLength;
    }

    @OneToMany(mappedBy = "fanout", cascade= CascadeType.ALL)
    public List<FanoutEntry> getFanoutEntrys() {
        return fanoutEntrys;
    }

    public void setFanoutEntrys(List<FanoutEntry> fanoutEntrys) {
        this.fanoutEntrys = fanoutEntrys;
    }

    @Override
    @javax.persistence.Transient
    public List<Long> getHashes() {
        List<Long> list = new ArrayList<>();
        for( FanoutEntry fe : fanoutEntrys) {
            list.add(fe.getChunkHash());
        }
        return list;
    }
    
    
}
