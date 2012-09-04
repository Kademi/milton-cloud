package io.milton.cloud.server.sync.push;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author brad
 */
public class ClientMessageFilterImpl implements ClientMessageFilter {

    @Override
    public void handleNotification( ChannelListener listener, UUID sourceId, Serializable msg ) {
        listener.handleNotification( sourceId, msg );
    }

    @Override
    public void memberRemoved( ChannelListener listener, UUID sourceId ) {
        listener.memberRemoved( sourceId );
    }

    @Override
    public void onConnect( ChannelListener listener ) {
        listener.onConnect();
    }

}
