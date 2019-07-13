package refactor.network;

import refactor.GlobalSettings;
import refactor.exception.*;
import refactor.message.Message;
import refactor.message.MessageDecoder;
import refactor.message.MessageRouter;
import refactor.protocol.notifications.AbstractNotifiable;
import refactor.protocol.notifications.MessageNotification;
import refactor.protocol.notifications.Notification;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.AlreadyConnectedException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// TODO Javadoc this class
public class TCP extends AbstractNotifiable {

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    // Map of existing connections, and the channel responsible for them
    private Map<SocketAddress, SocketChannel> connections;

    private static TCP tcp = new TCP();

    public static TCP tcp() {
        return tcp;
    }

    private TCP() {
        super();
        connections = new HashMap<>();
        try {
            initServerSocketChannel();
        } catch(IOException ioe) {
            // TODO
            System.exit(1);
        }
    }

    private void initServerSocketChannel()
            throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open()
                .bind(GlobalSettings.localAddress());
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Spawning Thread for TCP to listen to socket.");
        GlobalSettings.FIXED_THREAD_POOL.submit(this::listenToSelector);
    }

    private void accept()
            throws IOException {
        if(GlobalSettings.DEBUGGING_LEVEL >= 4)
            System.out.println(serverSocketChannel.getLocalAddress() + " is receiving a connection");
        SocketChannel socketChannel = serverSocketChannel.accept();
        if(GlobalSettings.DEBUGGING_LEVEL >= 4)
            System.out.println("Connection from " + socketChannel.getRemoteAddress());
        setupNewChannel(socketChannel);
        if(GlobalSettings.DEBUGGING_LEVEL >= 4)
            System.out.println("Connection successful");
    }

    private void finishConnection(SelectableChannel selectableChannel)
            throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectableChannel;
        if(socketChannel.finishConnect())
            setupNewChannel(socketChannel);
    }

    private void setupNewChannel(SocketChannel socketChannel)
            throws IOException {
        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.register(selector, SelectionKey.OP_READ);
        connections.put(socketChannel.getRemoteAddress(), socketChannel);
    }

    private void read(SelectableChannel selectableChannel)
            throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectableChannel;
        SocketAddress sender = socketChannel.getRemoteAddress();
        if(!(sender instanceof InetSocketAddress))
            throw new SocketException();
        ByteBuffer totalSizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        // If exactly 4 bytes couldn't be read, something went wrong
        if(socketChannel.read(totalSizeBuffer) != 4)
            throw new IOException();
        ByteBuffer messageBuffer = ByteBuffer.allocate(((ByteBuffer) totalSizeBuffer.flip()).getInt());
        socketChannel.read(messageBuffer);
        try {
            Message message = MessageDecoder.decodeMessage(messageBuffer);
            MessageRouter.getRouter().routeMessage(message);
        } catch(NullMessageException | InvalidMessageTypeException | SingletonIsNullException e) {
            // TODO
            System.exit(1);
        }
    }

    private void listenToSelector() {
        try {
            for(;;) {
                selector.select();
                Iterator<SelectionKey> keyIt = selector.selectedKeys().iterator();

                if(GlobalSettings.DEBUGGING_LEVEL >= 4)
                    System.out.println("TCP Selector selected " + selector.selectedKeys().size() + " keys.");

                while (keyIt.hasNext()) {
                    SelectionKey key = keyIt.next();
                    SelectableChannel selectableChannel = key.channel();

                    if(key.isAcceptable())
                        accept();
                    else if(key.isConnectable())
                        finishConnection(selectableChannel);
                    else if(key.isReadable())
                        read(selectableChannel);

                    keyIt.remove();
                }
            }
        } catch(IOException ioe) {
            // TODO
            System.exit(1);
        }
    }

    public void connect(InetSocketAddress remoteNodeAddress)
            throws AlreadyConnectedException {
        try {
            // Check for the existence of this connection, in the map of connections
            SocketChannel channel = connections.get(remoteNodeAddress);
            // Throws an exception, if this connection is already established
            if(channel != null)
                throw new AlreadyConnectedException();

            if(GlobalSettings.DEBUGGING_LEVEL >= 4)
                System.out.println("Creating new channel between local: " + GlobalSettings.localAddress() +
                        " and remote: " + remoteNodeAddress);
            channel = SocketChannel.open()
                    .setOption(StandardSocketOptions.SO_KEEPALIVE, true)
                    .bind(new InetSocketAddress(0));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            // This is a non-blocking connect, and has to be, because this is the same thread that is reading
            // incoming messages from the network, and it cannot wait for this connection to be established
            System.out.println("Connecting to " + remoteNodeAddress);
            channel.connect(remoteNodeAddress);
        } catch(IOException ioe) {
            // TODO
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void handleNotification(Notification notification) {
        try {
            if (!(notification instanceof MessageNotification)) {
                if (GlobalSettings.DEBUGGING_LEVEL >= 4)
                    System.out.println("TCP got wrong notification, expected a MessageNotification");
                return;
            }
            Message messageToSend = ((MessageNotification) notification).message();
            // If the Message has no destination, it can't be sent
            if (messageToSend.getDestination() == null || messageToSend.getDestination().isUnresolved())
                throw new MessageHasNoDestinationException();
            SocketChannel socketChannel = connections.get(messageToSend.getDestination());
            // Something went wrong in the upper layers, probably
            if (socketChannel == null)
                throw new ConnectionNotEstablishedException();
            ByteBuffer messageBuffer = messageToSend.encode();
            // First send the total size of the Message, so the receiving Node's TCP layer knows how much to read
            ByteBuffer totalSizeBuffer = ByteBuffer.allocate(Integer.BYTES);
            totalSizeBuffer.putInt(0, messageBuffer.capacity());
            socketChannel.write(totalSizeBuffer);
            // Then send the Message itself
            socketChannel.write(messageBuffer);
        } catch(IOException | MessageTooLargeException ioe) {
            // TODO
            System.exit(1);
        }
    }
}
