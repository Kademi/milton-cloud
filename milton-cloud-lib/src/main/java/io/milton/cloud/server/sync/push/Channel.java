package io.milton.cloud.server.sync.push;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

/**
 *
 * @author brad
 */
public interface Channel {

    /**
     * Broadcast a message to the cluster
     * 
     * @param msg
     */
    void sendNotification( Serializable msg );

    /**
     * Send a message to a particular cluster member
     *
     * @param destination - the UUID to send to
     * @param msg - the message to send
     */
    void sendNotification( UUID destination, Serializable msg );

    void registerListener( ChannelListener channelListener );

    void removeListener( ChannelListener channelListener );

    /**
     * 
     * @return - the address that this channel is bound to
     */
    InetAddress getLocalAddress();
}
