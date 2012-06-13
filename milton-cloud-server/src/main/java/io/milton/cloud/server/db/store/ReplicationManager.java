package io.milton.cloud.server.db.store;

import java.util.UUID;

/**
 *
 * @author brad
 */
public interface ReplicationManager {
    void newBlob(long volumeInstanceId, long hash);
}
