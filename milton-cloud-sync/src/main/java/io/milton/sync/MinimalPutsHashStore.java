package io.milton.sync;

import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

/**
 *
 * @author brad
 */


public class MinimalPutsHashStore implements HashStore{

    private final HashStore wrapped;

    public MinimalPutsHashStore(HashStore wrapped) {
        this.wrapped = wrapped;
    }
    
    
    
    @Override
    public void setChunkFanout(String string, List<String> list, long l) {
        if( !wrapped.hasChunk(string)) {
            wrapped.setChunkFanout(string, list, l);
        }
    }

    @Override
    public void setFileFanout(String string, List<String> list, long l) {
        if( !wrapped.hasFile(string)) {
            wrapped.setFileFanout(string, list, l);
        }
    }

    @Override
    public Fanout getFileFanout(String string) {
        if( wrapped.hasFile(string)) {
            return wrapped.getFileFanout(string);
        }
        return null;
    }

    @Override
    public Fanout getChunkFanout(String string) {
        if( wrapped.hasChunk(string)) {
            return wrapped.getChunkFanout(string);
        }
        return null;
    }

    @Override
    public boolean hasChunk(String string) {
        return wrapped.hasChunk(string);
    }

    @Override
    public boolean hasFile(String string) {
        return wrapped.hasFile(string);
    }
    
}
