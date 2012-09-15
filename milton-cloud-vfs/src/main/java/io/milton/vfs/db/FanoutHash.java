package io.milton.vfs.db;

import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hashsplit4j.api.Fanout;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;

/**
 * Represents either a list of other fanout hashes, or a list of blob hashes
 *
 * Generally a file will have a hash which is used to locate the file fanout.
 * From the file fanout you get a list of chunk fanouts, which contains a list
 * of blob hashes
 *
 * Building the content for a file consists of walking over that graph,
 * outputting bytes as you go
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "fanoutHash"})}// item names must be unique within a directory
)
public class FanoutHash implements Serializable, Fanout {

    public static void insertFanout(String type, String hash, List<String> childCrcs, long actualContentLength, Session session) {
        FanoutHash fanout = new FanoutHash();
        fanout.setType(type);
        fanout.setFanoutHash(hash);
        fanout.setActualContentLength(actualContentLength);
        List<FanoutEntry> list = new ArrayList<>(childCrcs.size());
        for (String l : childCrcs) {
            FanoutEntry fe = new FanoutEntry();
            fe.setChunkHash(l);
            fe.setFanout(fanout);
            list.add(fe);
        }
        fanout.setFanoutEntrys(list);
        session.save(fanout);
    }

    /**
     *
     * @param hash
     * @param type - c for chunk, or f for file
     * @param session
     * @return
     */
    public static FanoutHash findByHashAndType(String hash, String type, Session session) {
        Criteria crit = session.createCriteria(FanoutHash.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("fanoutHash", hash));
        crit.add(Restrictions.eq("type", type));
        return DbUtils.unique(crit);
    }
    
    private Long id;
    private String fanoutHash;
    private String type; // c=chunk, f=file    
    private long actualContentLength;
    private List<FanoutEntry> fanoutEntrys;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    

    @Column(nullable=false)
    public String getFanoutHash() {
        return fanoutHash;
    }

    public void setFanoutHash(String hash) {
        this.fanoutHash = hash;
    }

    /**
     * The fanout type, either can be a chunk fanout = c, or a file fanout=f
     *
     * File fanouts contain hashes of other fanouts, while chunk fanouts
     * contain hashes of blobs
     *
     * @return
     */
    @Column(nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }    

    @Column(nullable = false)
    @Override
    public long getActualContentLength() {
        return actualContentLength;
    }

    public void setActualContentLength(long actualContentLength) {
        this.actualContentLength = actualContentLength;
    }

    @OneToMany(mappedBy = "fanout", cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<FanoutEntry> getFanoutEntrys() {
        return fanoutEntrys;
    }

    public void setFanoutEntrys(List<FanoutEntry> fanoutEntrys) {
        this.fanoutEntrys = fanoutEntrys;
    }

    @Override
    @javax.persistence.Transient
    public List<String> getHashes() {
        List<String> list = new ArrayList<>();
        for (FanoutEntry fe : fanoutEntrys) {
            list.add(fe.getChunkHash());
        }
        return list;
    }
}
