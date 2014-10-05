package io.milton.vfs.data;

import io.milton.event.Event;
import java.util.List;

/**
 * 
 *
 * @author brad
 */
public class NodesChangedEvent implements Event{
    private final DataSession sourceSession;
    private final List<DataSession.DirectoryNode> directoryNodes;

    public NodesChangedEvent(DataSession sourceSession, List<DataSession.DirectoryNode> directoryNodes) {
        this.sourceSession = sourceSession;
        this.directoryNodes = directoryNodes;
    }

    public List<DataSession.DirectoryNode> getDirectoryNodes() {
        return directoryNodes;
    }     

    public DataSession getSourceSession() {
        return sourceSession;
    }
    
    
}
