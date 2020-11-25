package io.milton.sync.triplets;

import java.io.File;

/**
 *
 * @author brad
 */


public interface SyncHashCache {
    public String get(File file);
    public void put(File file, String hash);
}
