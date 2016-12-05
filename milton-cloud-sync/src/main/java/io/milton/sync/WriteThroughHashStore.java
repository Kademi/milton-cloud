package io.milton.sync;

import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

/**
 *
 * @author brad
 */
public class WriteThroughHashStore implements HashStore{
    private final HashStore primary;
    private final HashStore secondary;

    public WriteThroughHashStore(HashStore primary, HashStore secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public void setChunkFanout(String hash, List<String> blobHashes, long actualContentLength) {
        primary.setChunkFanout(hash, blobHashes, actualContentLength);
        secondary.setChunkFanout(hash, blobHashes, actualContentLength);
    }

    @Override
    public void setFileFanout(String hash, List<String> fanoutHashes, long actualContentLength) {
        primary.setFileFanout(hash, fanoutHashes, actualContentLength);
        secondary.setFileFanout(hash, fanoutHashes, actualContentLength);
    }

    @Override
    public Fanout getFileFanout(String fileHash) {
        Fanout f = primary.getFileFanout(fileHash);
        return f;
    }

    @Override
    public Fanout getChunkFanout(String fanoutHash) {
        return primary.getChunkFanout(fanoutHash);
    }

    @Override
    public boolean hasChunk(String fanoutHash) {
        return secondary.hasChunk(fanoutHash);
    }

    @Override
    public boolean hasFile(String fileHash) {
        return secondary.hasFile(fileHash);
    }


}
