package io.milton.vfs.content;

import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import io.milton.vfs.db.FanoutHash;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class DbHashStore implements HashStore{
 
    
    @Override
    public void setChunkFanout(String hash, List<String> childCrcs, long actualContentLength) {
        if( hasChunk(hash)) {
            return ;
        }
        FanoutHash.insertFanout("c", hash, childCrcs, actualContentLength, SessionManager.session());
    }

    @Override
    public Fanout getChunkFanout(String hash) {
        return getFanout(hash, "c");
    }

    @Override
    public boolean hasChunk(String hash) {
        return getFanout(hash, "c") != null;
    }


    @Override
    public void setFileFanout(String hash, List<String> fanoutHashes, long actualContentLength) {
        if( hasFile(hash)) {
            return ;
        }
        FanoutHash.insertFanout("f", hash, fanoutHashes, actualContentLength, SessionManager.session());
    }

    @Override
    public Fanout getFileFanout(String fileHash) {
        return getFanout(fileHash, "f");
    }

    @Override
    public boolean hasFile(String fileHash) {
        return getFileFanout(fileHash) != null;
    }    
    
    private Fanout getFanout(String hash, String type) {
        return FanoutHash.findByHashAndType(hash, type, SessionManager.session());
    }         
}
