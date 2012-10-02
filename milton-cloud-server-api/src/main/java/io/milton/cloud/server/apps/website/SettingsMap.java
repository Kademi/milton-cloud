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
package io.milton.cloud.server.apps.website;

import io.milton.vfs.db.NvPair;
import io.milton.vfs.db.Repository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author brad
 */
public class SettingsMap implements Map<String, String> {
    private final Repository repository;

    public SettingsMap(Repository repository) {
        this.repository = repository;
    }

    @Override
    public String get(Object key) {
//        String s = repository.getAttribute(key.toString());
//        return s;
        return null;
    }

    @Override
    public int size() {
//        if (repository.getNvPairs() != null) {
//            return repository.getNvPairs().size();
//        } else {
            return 0;
//        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> set = new HashSet<>();
//        if (repository.getNvPairs() != null) {
//            for (NvPair nv : repository.getNvPairs()) {
//                final NvPair pair = nv;
//                Entry<String, String> e = new Entry<String, String>() {
//
//                    @Override
//                    public String getKey() {
//                        return pair.getName();
//                    }
//
//                    @Override
//                    public String getValue() {
//                        return pair.getPropValue();
//                    }
//
//                    @Override
//                    public String setValue(String value) {
//                        throw new UnsupportedOperationException("Not supported yet.");
//                    }
//                };
//                set.add(e);
//            }
//        }
        return set;
    }
    
}
