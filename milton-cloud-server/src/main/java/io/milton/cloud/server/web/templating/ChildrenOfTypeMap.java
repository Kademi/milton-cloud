package io.milton.cloud.server.web.templating;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import io.milton.cloud.server.web.ResourceList;

/**
 *
 * @author brad
 */
public class ChildrenOfTypeMap implements Map<String,ResourceList>{

    private final ResourceList list;

    public ChildrenOfTypeMap(ResourceList list) {
        this.list = list;
    }
    
    
    
    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return true;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceList get(Object key) {
        return list.ofType(key.toString());               
    }

    @Override
    public ResourceList put(String key, ResourceList value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceList remove(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends ResourceList> m) {
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
    public Collection<ResourceList> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<String, ResourceList>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
