package io.milton.cloud.server.sync.push;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xsocket.IDataSink;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.INonBlockingConnection;

/**
 * Encoder and decoder for objects being sent via the TCP channel
 *
 * Uses object serialization, but wrapped in a fixed length messages so we can
 * forward without needing to deserialise.
 *
 * @author brad
 */
public class TcpObjectCodec {

    private static final Logger log = LoggerFactory.getLogger( TcpObjectCodec.class );
    private int maxMessageSizeBytes = 100000; // 100k max message size

    /**
     * defaults maxMessageSizeBytes to 100k
     */
    public TcpObjectCodec() {
    }

    public void encodeClientToHub( UUID dest, IDataSink sink, Serializable data ) throws IOException {
        byte[] arr = encodeToBytes( data );
        if( arr.length > maxMessageSizeBytes ) {
            throw new RuntimeException( "Message is too large: " + data.getClass().getCanonicalName() + " Size is: " + arr.length + " Message class: " + data.getClass() );
        } else if( arr.length > maxMessageSizeBytes / 4 ) {
            log.warn( "Fairly large mesasge: " + data.getClass().getCanonicalName() + " Size:" + arr.length + " Message class: " + data.getClass() );
            throw new RuntimeException( "too big" );
        }

        encodeBytes( sink, dest, arr );
    }

    public void encodeHubToClient( UUID source, IDataSink sink, byte[] arr ) throws IOException {
//        if( data instanceof byte[] ) {
//            throw new RuntimeException( "sending byte array");
//        }
//        byte[] arr = encodeToBytes( data );
        if( arr.length > maxMessageSizeBytes ) {
            throw new RuntimeException( "Message is too large: Size is: " + arr.length );
        } else if( arr.length > maxMessageSizeBytes / 4 ) {
            log.warn( "Fairly large mesasge: Size:" + arr.length );
            throw new RuntimeException( "too big" );
        }

        encodeBytes( sink, source, arr );
    }

    public byte[] encodeToBytes( Serializable data ) {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            oos = new ObjectOutputStream( bout );
            oos.writeObject( data );
            byte[] arr = bout.toByteArray();
            return arr;
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        } finally {
            try {
                oos.close();
            } catch( IOException ex ) {
                log.error( "exception closing stream", ex );
            }
        }
    }

    /**
     *
     * @param out
     * @param id - either source or destination. May be null for hub to client
     * @param data
     * @throws IOException
     */
    public synchronized void encodeBytes( IDataSink out, UUID id, byte[] data ) throws IOException {
        // Write the length of the destination (zero length if dest is null) then the destination
        try {
            String sId = "";
            if( id != null ) {
                sId = id.toString();
            }
            out.write( sId.length() );
            if( sId.length() > 0 ) {
                out.write( sId );
            }

            // Write the length of the message, then the message itself
            out.write( data.length );
            out.write( data );
        } catch( IOException iOException ) {
            log.warn( "encodeBytes: ioexception" );
            throw iOException;
        } catch( BufferOverflowException bufferOverflowException ) {
            log.warn( "encodeBytes: buf overflow" );
            throw bufferOverflowException;
        }
    }

    public IdAndArray decodeRaw( INonBlockingConnection con ) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
        con.markReadPosition();
        byte[] data;
        UUID id = null;
        try {
            int idLength = con.readInt();
            if( idLength > 0 ) {
                String sId = con.readStringByLength( idLength );
                id = UUID.fromString( sId );
            }

            int length = con.readInt();
            data = con.readBytesByLength( length );
        } catch( BufferUnderflowException e ) {
            con.resetToReadMark();
            throw e;
        }

        return new IdAndArray( id, data );
    }

    public IdAndObject decode( INonBlockingConnection con ) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException, ClassNotFoundException {
        IdAndArray idAndArray = decodeRaw( con );

        ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( idAndArray.data ) );
        Object o = oin.readObject();
        Serializable payload = (Serializable) o;

        return new IdAndObject( idAndArray.id, payload );
    }

    public int getMaxMessageSizeBytes() {
        return maxMessageSizeBytes;
    }

    public void setMaxMessageSizeBytes( int maxMessageSizeBytes ) {
        this.maxMessageSizeBytes = maxMessageSizeBytes;
    }

    public class IdAndObject {

        final UUID id;
        final Serializable data;

        public IdAndObject( UUID id, Serializable data ) {
            this.id = id;
            this.data = data;
        }

        public Serializable getData() {
            return data;
        }

        public UUID getId() {
            return id;
        }
        
        
        
        
    }

    public class IdAndArray {

        final UUID id;
        final byte[] data;

        public IdAndArray( UUID id, byte[] data ) {
            this.id = id;
            this.data = data;
        }

        public UUID getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }
        
        
    }
}
