package io.milton.cloud.server.db.store;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import io.milton.cloud.server.db.utils.SessionManager;
import io.milton.cloud.server.db.Volume;
import io.milton.cloud.server.db.VolumeInstance;


/**
 * A very smiple implementation which uses a single Volume
 * 
 * This implementation ignores the previouslyFailed parameter and ignores
 * capacity, cost and online values
 *
 * @author brad
 */
public class SingleVolumeInstanceAllocator implements VolumeInstanceAllocator {

    private Long theVolumeId;
    
    private int replicationFactor = 2;

    @Override
    public synchronized VolumeInstance nextWriteInstance(List<VolumeInstance> previouslyFailed) {
        Session session = SessionManager.session();
        if (theVolumeId == null) {
            // select a volume
            List<Volume> allVolumes = Volume.findAll();
            Volume theVolume;
            if( allVolumes.isEmpty() ) {
                theVolume = createVolume(session);
            } else {
                theVolume = allVolumes.get(0);
            }
            theVolumeId = theVolume.getId();
        }
        Volume theVolume = Volume.get(theVolumeId);
        return findOrCreateInstance(session, theVolume, true);

    }
    @Override
    public VolumeInstance getReaderInstance(long volumeId, List<VolumeInstance> previouslyFailed) {
        Session session = SessionManager.session();
        if (theVolumeId == null) {
            // select a volume
            List<Volume> allVolumes = Volume.findAll();
            Volume theVolume;
            if( allVolumes.isEmpty() ) {
                theVolume = createVolume(session);
            } else {
                theVolume = allVolumes.get(0);
            }
            theVolumeId = theVolume.getId();
        }
        Volume theVolume = Volume.get(theVolumeId);
        return findOrCreateInstance(session, theVolume, false);
    }


    private Volume createVolume(Session session) {
        Volume volume = new Volume();
        volume.setTargetCapacity(Long.MAX_VALUE);
        volume.setUsedBytes(0);
        volume.setInstances(new ArrayList<VolumeInstance>());
        SessionManager.session().save(volume);
        
        // Add some volume instances
        List<VolumeInstance> unmounted = VolumeInstance.findByNullVolume(SessionManager.session());
        for( int i=0; i<replicationFactor; i++ ) {
            if( !unmounted.isEmpty() ) {
                VolumeInstance vi = unmounted.remove(0);
                vi.setVolume(volume);
                volume.getInstances().add(vi);
                session.save(vi);
            }
        }
        
        return volume;
    }

    private VolumeInstance findOrCreateInstance(Session session, Volume v, boolean autocreate) {
        List<VolumeInstance> instances = v.getInstances();
        if( instances == null ) {
            instances = new ArrayList<>();
            v.setInstances(instances);
        }
        if( instances.isEmpty() && autocreate ) {
            // Find an unmounted instance and assign it to the volume
            List<VolumeInstance> unmounted = VolumeInstance.findByNullVolume(SessionManager.session());
            if( unmounted.isEmpty() ) {
                return null;
            } else {
                VolumeInstance vi = unmounted.get(0);
                vi.setVolume(v);
                instances.add(vi);
                session.save(vi);
                return vi;
            }            
        }
        if( instances.size() > 0 ) {
            return instances.get(0);
        } else {
            return null;
        }
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }
    
    
}
