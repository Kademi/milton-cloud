package io.milton.cloud.server.db.store;

import java.util.*;
import org.hashsplit4j.api.BlobStore;
import io.milton.cloud.server.db.BlobHash;
import io.milton.cloud.server.db.utils.SessionManager;
import io.milton.cloud.server.db.VolumeInstance;

/**
 * Allocates blobs to volumes.
 *
 * A volume is a storage facility for storing blob data. A Volume might simply
 * be a directory in a traditional filesystem, or it might be something more
 * sophisticated like a distributed filesystem with arbitrary replication
 * factors, corruption detection and self-healing
 *
 * @author brad
 */
public class HAVolumeBlobStore implements BlobStore {

    private final VolumeInstanceAllocator allocator;
    private final Map<String, VolumeInstanceType> mapOfInstanceTypes;
    private final ReplicationManager replicationManager;

    public HAVolumeBlobStore(VolumeInstanceAllocator allocator, List<VolumeInstanceType> instanceTypes, SessionManager sessionManager) {
        this.allocator = allocator;        
        Map<String,VolumeInstanceType> map = new HashMap<>();
        for( VolumeInstanceType vit : instanceTypes ) {
            map.put(vit.getTypeId(), vit);
        }
        this.mapOfInstanceTypes = Collections.unmodifiableMap(map);
        DefaultReplicationManager rm = new DefaultReplicationManager(sessionManager, mapOfInstanceTypes);
        this.replicationManager = rm;
        rm.start();
    }
    
    
    @Override
    public void setBlob(long hash, byte[] bytes) {
        BlobHash blobHash = BlobHash.findByHash(hash);
        if (blobHash != null) {
            return;
        }

        long volumeId;
        try {
            volumeId = writeBytes(hash, bytes);
        } catch (Exception ex) {
            // TODO: try a different VI
            throw new RuntimeException(ex);
        }

        blobHash = new BlobHash();
        blobHash.setBlobHash(hash);
        blobHash.setVolumeId(volumeId);

        SessionManager.session().save(blobHash);
    }

    @Override
    public byte[] getBlob(long hash) {
        // Find its volume
        BlobHash blobHash = (BlobHash) SessionManager.session().get(BlobHash.class, hash);
        if (blobHash == null) {
            return null;
        } else {
            VolumeInstance vi = allocator.getReaderInstance(blobHash.getVolumeId(), null);
            if( vi == null ) {
                throw new RuntimeException("Couldnt allocate a volume instance to read from for volume: " + blobHash.getVolumeId() + " Please check configuration and availability of drives");
            }
            VolumeInstanceType type = mapOfInstanceTypes.get(vi.getInstanceType());
            if (type == null) {
                throw new RuntimeException("Couldnt find location type: " + type + " please check app server configuration");
            }
            try {
                return type.getBlob(vi.getLocation(), hash);
            } catch (VolumeInstanceException ex) {
                // TODO: implement retries
                throw new RuntimeException("Exception accessing volume instance: " + vi.getId(), ex);
            }
        }
    }

    @Override
    public boolean hasBlob(long hash) {
        BlobHash blobHash = (BlobHash) SessionManager.session().get(BlobHash.class, hash);
        return (blobHash != null);
    }

    /**
     *
     *
     * @param hash - hash of the data to store
     * @param bytes - the bytes to store
     * @return - ID of the volume (not volume instance!) written to
     * @throws Exception
     */
    public long writeBytes(long hash, byte[] bytes) throws Exception {
        VolumeInstance vi = allocator.nextWriteInstance(null); // todo: imlpement retries, with previouslyFailed list
        VolumeInstanceType type = mapOfInstanceTypes.get(vi.getInstanceType());
        if (type == null) {
            throw new Exception("Couldnt find location type: " + type);
        }
        type.setBlob(vi.getLocation(), hash, bytes);
        replicationManager.newBlob(vi.getId(), hash);
        return vi.getVolume().getId();
    }
}
