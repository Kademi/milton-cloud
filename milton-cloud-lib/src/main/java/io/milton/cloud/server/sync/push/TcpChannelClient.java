package io.milton.cloud.server.sync.push;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;

/**
 *
 * @author brad
 */
public class TcpChannelClient implements Channel, LocalAddressAccessor {

    private static final Logger log = LoggerFactory.getLogger( TcpChannelClient.class );
    private final InetAddress hubAddress;
    private final int hubPort;
    private final List<ChannelListener> channelListeners;
    private boolean running;
    private ClientMessageFilter filter = new ClientMessageFilterImpl();
    /**
     * Keeps checking for a connection
     */
    private Thread thMonitor;
    private Thread thSender;
    private LinkedBlockingQueue<QueuedMessage> sendQueue = new LinkedBlockingQueue<>();
    private INonBlockingConnection con;
    /**
     * Just used for sending
     */
    private final TcpObjectCodec codec = new TcpObjectCodec();

    public TcpChannelClient( String hubAddress, int hubPort ) throws UnknownHostException {
        this( InetAddress.getByName( hubAddress ), hubPort, new ArrayList<ChannelListener>() );
    }

    public TcpChannelClient( String hubAddress, int hubPort, List<ChannelListener> channelListeners ) throws UnknownHostException {
        this( InetAddress.getByName( hubAddress ), hubPort, channelListeners );
    }

    public TcpChannelClient( InetAddress hubAddress, int hubPort, List<ChannelListener> channelListeners ) {
        this.hubAddress = hubAddress;
        this.hubPort = hubPort;
        this.channelListeners = channelListeners;
    }

    public void start() {
        log.warn( "start: " + this.getClass().getCanonicalName() );
        running = true;

        thSender = new Thread( new QueueSender(), "TcpChannelClientSender" );
        thSender.setDaemon( true );

        thMonitor = new Thread( new ConnectionMonitor(), "TcpChannelClientMonitor" );
        thMonitor.setDaemon( true );

        thSender.start();
        thMonitor.start();
    }

    public void stop() {
        log.warn( "stop: " + this.getClass().getCanonicalName() );
        running = false;
        thSender.interrupt();
        thMonitor.interrupt();
        disconnect();
    }

    @Override
    public void sendNotification( Serializable msg ) {
//        log.debug( "sendNotification: " + msg.getClass() + " queue: " + sendQueue.size() );
        sendQueue.add( new QueuedMessage( null, msg ) );
    }

    @Override
    public void sendNotification( UUID destination, Serializable msg ) {
//        log.debug( "sendNotification2: " + msg.getClass() + " queue: " + sendQueue.size() );
        sendQueue.add( new QueuedMessage( destination, msg ) );
    }

    @Override
    public void registerListener( ChannelListener channelListener ) {
        channelListeners.add( channelListener );
    }

    @Override
    public void removeListener( ChannelListener channelListener ) {
        channelListeners.remove( channelListener );
    }

    public ClientMessageFilter getFilter() {
        return filter;
    }

    public void setFilter( ClientMessageFilter filter ) {
        this.filter = filter;
    }

    

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if( running ) {
            log.debug( "finalize called, but not stopped. Attempt to disconnect.." );
            disconnect();
        }
    }

    @Override
    public InetAddress getLocalAddress() {
        if( con != null ) {
            return con.getLocalAddress();
        } else {
            return null;
        }
    }

    private synchronized boolean isConnected() {
        return con != null && con.isOpen();
    }

    private synchronized void connect() {
        boolean didConnect = false;
        log.debug( "attempt to connect: " + hubAddress + ":" + hubPort );
        try {
            MessageReceiver rcv = new MessageReceiver();
            Map<String, Object> options = new HashMap<>();
            options.put( IConnection.SO_REUSEADDR, true );
            con = new NonBlockingConnection( hubAddress, hubPort, rcv, options );
            if( !con.isOpen() ) {
                log.debug( "socket did not connect" );
                con = null;
                return;
            } else {
                // test the socket
                try {
                    codec.encodeClientToHub( null, con, new AreYouThere() );
                    didConnect = true;
                    log.debug( ".. connected ok" );
                } catch( IOException ex ) {
                    log.debug( "connection is invalid" );
                    disconnect();
                }
            }

        } catch( IOException ex ) {
            log.warn( "Failed to connect to: " + hubAddress + ":" + hubPort + ". ex: " + ex.toString() );
            con = null;
        }
        if( didConnect ) {
            try {
                notifyConnected();
            } catch( Exception e ) {
                log.error( "exception in notifyConnected", e );
            }
        }
    }

    private synchronized void disconnect() {
        log.debug( "disconnect" );
        if( con != null ) {
            try {
                con.close();
            } catch( IOException ex ) {
                log.warn( "exception closing socket: " + ex );
            }
        }
        con = null;
    }

    private void notifyConnected() {
        log.debug( "notifyConnected" );
        for( ChannelListener l : channelListeners ) {
            try {
                filter.onConnect( l );
            } catch( Exception e ) {
                log.error( "Exception in memberRemoved listener: " + l.getClass(), e );
            }
        }
    }

    private class ConnectionMonitor implements Runnable {

        @Override
        public void run() {
            try {
                while( running ) {
                    if( !isConnected() ) {
                        connect();
                    }
                    Thread.sleep( 5000 );
                }
            } catch( InterruptedException ex ) {
                log.warn( "connection monitor interrupted" );
            }
        }
    }

    private class MessageReceiver implements IDataHandler {

        @Override
        public boolean onData( INonBlockingConnection con ) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            try {
                TcpObjectCodec.IdAndObject idAndObject = codec.decode( con );
                handleNotification( idAndObject.id, idAndObject.data );
            } catch( ClassNotFoundException ex ) {
                log.info( "Discarding message because of unknown class: " + ex.getMessage() );
            } catch( java.nio.channels.ClosedChannelException ex ) {
                log.warn( "channel closed reading from hub: ", ex );
                disconnect();
            } catch( IOException ex ) {
                log.warn( "ioexception reading from hub: ", ex );
                disconnect();
            }
            return true;
        }

        public void handleNotification( final UUID sourceId, final Serializable data ) {
//            log.debug( "handleNotification: " + data.getClass());
            if( data instanceof MemberRemoved ) {
                for( ChannelListener l : channelListeners ) {
                    try {
                        filter.memberRemoved( l, sourceId );
                    } catch( Exception e ) {
                        log.error( "Exception in memberRemoved listener: " + l.getClass(), e );
                    }
                }
            } else if( data instanceof AreYouThere ) {
                // ignore
            } else {
//                log.debug( "handleNotification: " + data.getClass() );
                for( ChannelListener l : channelListeners ) {
                    try {
                        filter.handleNotification( l, sourceId, data );
                    } catch( Exception e ) {
                        log.error( "Exception in listener: " + l.getClass(), e );
                    }
                }
            }
        }
    }

    private class QueueSender implements Runnable {

        public void run() {
            try {
                while( running ) {
                    if( isConnected() ) {
                        consume( sendQueue.take() );
                    } else {
                        Thread.sleep( 5000 );
                    }
                }
            } catch( InterruptedException ex ) {
                log.warn( "thread finished" );
            }
        }

        private void consume( QueuedMessage msg ) {
            if( con == null ) {
                log.warn( "QueueSender: socket gone" );
                sendQueue.add( msg );
            } else {
                try {
                    codec.encodeClientToHub( msg.dest, con, msg.data );
                } catch( IOException ex ) {
                    log.warn( "exception sending data", ex );
                    disconnect();
                    sendQueue.add( msg );
                }
            }
        }
    }

    private class QueuedMessage {

        UUID dest;
        Serializable data;

        public QueuedMessage( UUID dest, Serializable data ) {
            this.dest = dest;
            this.data = data;
        }
    }
}
