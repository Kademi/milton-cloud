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
package io.milton.sync;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hashsplit4j.api.HashCache;

/**
 * Uses ConcurrentLinkedHashMap to keep a LRU cache of hashes
 *
 * @author brad
 */
public class MemoryHashCache implements HashCache {

    private final ConcurrentMap<String, String> cache;
    
    public MemoryHashCache() {
        cache = new ConcurrentLinkedHashMap.Builder<String, String>()
                .maximumWeightedCapacity(5000)
                .initialCapacity(2000)
                .build();
    }

    @Override
    public boolean hasHash(String key) {
        return cache.containsKey(key);
    }

    @Override
    public void setHash(String key) {
        cache.put(key, key);
    }
}
