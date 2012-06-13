package io.milton.cloud.server.db;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * A single VolumeInstance refers to a physical location at which bytes can be
 * stored, such as a directory in a file system or a remote HTTP server
 *
 * These are grouped together to form a Volume. A volume will replicate data
 * among the VolumeInstances in the Volume
 *
 * @author brad
 */
@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"location", "instanceType"}))
public class VolumeInstance implements Serializable {

    public static VolumeInstance find(Session session, String location, String instanceType) {
        Criteria crit = session.createCriteria(VolumeInstance.class);
        crit.add(Restrictions.eq("location", location));
        crit.add(Restrictions.eq("instanceType", instanceType));
        VolumeInstance vi = (VolumeInstance) crit.uniqueResult();
        return vi;
    }

    public static List<VolumeInstance> findByNullVolume(Session session) {
        Criteria crit = session.createCriteria(VolumeInstance.class);
        crit.add(Restrictions.isNull("volume"));
        List<VolumeInstance> list = new ArrayList<>();
        List oList = crit.list();
        if (oList != null) {
            for (Object o : oList) {
                list.add((VolumeInstance) o);
            }
        }
        return list;
    }

    public static VolumeInstance get(Session session, long volumeInstanceId) {
        return (VolumeInstance) session.get(VolumeInstance.class, volumeInstanceId);
    }
    
    private long id;
    private Volume volume;
    private String instanceType;
    private String location;
    private Long capacity;
    private int cost;
    private boolean online;
    private boolean lost;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = true) // only set when assigned
    public Volume getVolume() {
        return volume;
    }

    public void setVolume(Volume volume) {
        this.volume = volume;
    }

    /**
     * Identifies the VolumeInstanceType by its identifier
     *
     * @return
     */
    @Column(nullable = false)
    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    /**
     * Some path which makes sense to the appropriate VolumeInstanceType, which
     * points to a physical location
     *
     * @return
     */
    @Column(nullable = false)
    public String getLocation() {
        return location;
    }

    public void setLocation(String path) {
        this.location = path;
    }

    /**
     * Capacity of this store, in bytes
     *
     * @return
     */
    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    /**
     * Cost, as a indication of the expense to store and access data
     *
     * Generally VolumeInstances with a lower cost will be used preferentially
     * to higher cost ones
     *
     * @return
     */
    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    /**
     * True indicates that this volume is in service, and cannot be used to
     * store or retrieve blobs
     *
     * @return
     */
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * When set this indicates that the volume instance should be considered
     * destroyed and the system will begin replicating to another instance
     *
     * @return
     */
    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }
}
