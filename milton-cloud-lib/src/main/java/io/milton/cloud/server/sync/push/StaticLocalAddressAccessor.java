package io.milton.cloud.server.sync.push;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author brad
 */
public class StaticLocalAddressAccessor implements LocalAddressAccessor{

    private final InetAddress address;

    public StaticLocalAddressAccessor( InetAddress address ) {
        this.address = address;
    }

    public StaticLocalAddressAccessor(String s) throws UnknownHostException {
        this.address = InetAddress.getByName( s );
    }

    @Override
    public InetAddress getLocalAddress() {
        return address;
    }

}
