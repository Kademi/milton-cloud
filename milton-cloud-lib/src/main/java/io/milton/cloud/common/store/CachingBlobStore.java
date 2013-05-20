/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.common.store;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.hashsplit4j.api.BlobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A BlobStore which uses a MRU cache to store blobs in memory
 *
 * @author brad
 */
public class CachingBlobStore implements BlobStore {

    private static final Logger log = LoggerFactory.getLogger(CachingBlobStore.class);
    
    private final ConcurrentLinkedHashMap<String, byte[]> cache;
    private final BlobStore blobStore;
    private final int capacity;
    
    private long hits;
    private long misses;

    public CachingBlobStore(BlobStore blobStore, int capacity) {
        this.blobStore = blobStore;
        this.capacity = capacity;
        cache = new ConcurrentLinkedHashMap.Builder()
                .maximumWeightedCapacity(capacity)
                .build();
    }

    @Override
    public void setBlob(String hash, byte[] bytes) {
        blobStore.setBlob(hash, bytes);
        // NOTE: seems that items can be added faster then the cache can removed
        // So check if we're over capacity before adding
        if( cache.size() < capacity+20) {
            cache.putIfAbsent(hash, bytes);
        } else {
            log.warn("Cache is over capacity. Capacity=" + capacity + " size=" + cache.size());
        }
    }

    @Override
    public byte[] getBlob(String hash) {        
        byte[] arr = cache.get(hash);
        if( arr == null ) {
            arr = blobStore.getBlob(hash);
            if( arr != null ) {
                System.out.println("Caching blob store: hits=" + hits + " misses=" + misses);
                misses++;
                if( cache.size() < capacity+20) {
                    cache.putIfAbsent(hash, arr);
                }
            }
        } else {
            hits++;
        }
        return arr;
    }

    @Override
    public boolean hasBlob(String hash) {
        if( cache.containsKey(hash)) {
            hits++;
            return true;
        }
        boolean b = blobStore.hasBlob(hash);
        if( b) {
            misses++;
        }
        return b;
    }

    public int getCapacity() {
        return capacity;
    }
    
    
}
