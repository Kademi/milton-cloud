package io.milton.cloud.server.db.store;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import io.milton.cloud.server.db.utils.SessionManager;
import io.milton.cloud.server.db.VolumeInstance;

/**
 * Replication manager which uses a single thread and a blocking queue
 * 
 * You must call start to make it go!
 *
 * @author brad
 */
public class DefaultReplicationManager implements ReplicationManager {

    private final BlockingQueue<ReplicationItem> queue;
    
    private final SessionManager sessionManager;
    
    private final Map<String, VolumeInstanceType> mapOfInstanceTypes;

    private Thread th;
    
    public DefaultReplicationManager(SessionManager sessionManager, Map<String, VolumeInstanceType> mapOfInstanceTypes) {
        this.sessionManager = sessionManager;
        this.mapOfInstanceTypes = mapOfInstanceTypes;
        queue = new ArrayBlockingQueue(100000);
    }
    
    public void start() {
        th = Executors.defaultThreadFactory().newThread(new Consumer());
        th.start();
    }
    
    public void stop() {
        if( th != null ) {
            th.interrupt();
            th = null;
        }
    }

    @Override
    public void newBlob(long volumeInstanceId, long hash) {
        ReplicationItem item = new ReplicationItem(volumeInstanceId, hash);
        queue.add(item);
    }

    private void replicate(ReplicationItem item) throws VolumeInstanceException, InterruptedException {
        try {
            sessionManager.open();
            VolumeInstance viSource = VolumeInstance.get(SessionManager.session(), item.volumeInstanceId);
            
            // means that the source transaction has not yet been completed, so requeue and wait
            if( viSource.getVolume() == null || viSource.getVolume().getInstances() == null ) {
                Thread.sleep(100);                 
                queue.add(item);
                return ;
            }
            
            VolumeInstanceType sourceType = mapOfInstanceTypes.get(viSource.getInstanceType());
            byte[] arr = sourceType.getBlob(viSource.getLocation(), item.hash);
                                    
            for( VolumeInstance viDest : viSource.getVolume().getInstances()) {
                if( viDest.getId() == item.volumeInstanceId) {
                    // ignore, since it is the source
                } else {
                    VolumeInstanceType destType = mapOfInstanceTypes.get(viDest.getInstanceType());
                    destType.setBlob(viDest.getLocation(), item.hash, arr);
                }
            }
        } finally {
            sessionManager.close();            
        }
    }

    class Consumer implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        replicate(queue.take());
                    } catch (VolumeInstanceException ex) {
                        System.out.println("Couldnt process replication: " + ex);
                    }
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    private class ReplicationItem {

        final long volumeInstanceId;
        final long hash;

        ReplicationItem(long volumeInstanceId, long hash) {
            this.volumeInstanceId = volumeInstanceId;
            this.hash = hash;
        }
    }
}
