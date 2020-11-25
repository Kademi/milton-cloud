package io.milton.sync.triplets;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author brad
 */


public class MemorySyncHashCache implements SyncHashCache {

    private final Map<String,String> mapOfHashes = new HashMap<>();

    @Override
    public String get(File file) {
        String key = getKey(file);
        return mapOfHashes.get(key);
    }

    @Override
    public void put(File file, String hash) {
        String key = getKey(file);
        mapOfHashes.put(key, hash);
    }

    private String getKey(File file) {
        String key = file.getAbsolutePath() + "-" + file.length() + "-" + file.lastModified();
        return key;
    }
}
