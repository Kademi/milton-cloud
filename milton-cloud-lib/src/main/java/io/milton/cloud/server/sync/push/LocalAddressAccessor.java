package io.milton.cloud.server.sync.push;

import java.net.InetAddress;

/**
 * Represents a means to find the current IP address.
 *
 * Given that a machine may have many IP addresses, implementations must
 * be contextual, in some manner.
 *
 * For example, an implementation might provide the local adddress which
 * is in use by a Channel.
 *
 * Implementations should be performant, as the assumption is that getLocalAddress
 * may be called many times without caching by users of this class.
 *
 *
 * @author brad
 */
public interface LocalAddressAccessor {
    InetAddress getLocalAddress();
}
