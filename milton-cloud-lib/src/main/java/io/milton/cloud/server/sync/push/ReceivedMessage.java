package io.milton.cloud.server.sync.push;

import java.util.UUID;

/**
 *
 * @author brad
 */
public class ReceivedMessage {

    final UUID dest;
    final UUID source;
    final byte[] data;

    public ReceivedMessage(UUID dest, UUID source, byte[] data) {
        this.dest = dest;
        this.source = source;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public UUID getDest() {
        return dest;
    }

    public UUID getSource() {
        return source;
    }
}
