package io.milton.cloud.server.db;

import io.milton.cloud.server.db.utils.SessionManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.hibernate.Criteria;

/**
 * A volume maintains a dynamic membership of VolumeInstances, and replicates
 * data between them
 * 
 * A volume is persistent, and the volume ID is stored against blob hashes
 * in the database so the system can retrieve the data
 *
 * @author brad
 */
@Entity
public class Volume implements Serializable{

    public static List<Volume> findAll() {
        Criteria crit = SessionManager.session().createCriteria(Volume.class);
        List<Volume> list = new ArrayList<>();
        List oList = crit.list();
        if( oList != null ) {
            for( Object o : oList ) {
                list.add((Volume)o);
            }
        }
        return list;
    }

    public static Volume get(long theVolumeId) {
        return (Volume) SessionManager.session().get(Volume.class, theVolumeId);
    }
    private long id;
    
    private List<VolumeInstance> instances;
    
    private long targetCapacity;
    
    private long usedBytes;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany(mappedBy="volume")
    public List<VolumeInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<VolumeInstance> instances) {
        this.instances = instances;
    }

    /**
     * This is the expected capacity for the volume, set on initial
     * creation. Actual capacity depends on that of the member Volume instances,
     * but they will be selected with the intention of being compatible with the
     * target capacity
     * 
     * @return 
     * 
     */
    public long getTargetCapacity() {
        return targetCapacity;
    }

    public void setTargetCapacity(long targetCapacity) {
        this.targetCapacity = targetCapacity;
    }

    /**
     * Current count of bytes in this volume
     * 
     * @return 
     */
    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    
}
