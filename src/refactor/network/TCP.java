package refactor.network;

import refactor.GlobalSettings;
import refactor.exception.*;
import refactor.message.Message;
import refactor.message.MessageDecoder;
import refactor.message.MessageRouter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.AlreadyConnectedException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// TODO Javadoc this class
public class TCP {

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    // Map of existing connections, and the channel responsible for them
    private Map<SocketAddress, SocketChannel> connections;

    private BlockingQueue<Message> messagesToSend;
    
    public static TCP tcp = new TCP();
    
    public static TCP getTCP()
    		throws SingletonIsNullException {
    	if(tcp == null)
    		throw new SingletonIsNullException(TCP.class.getName());
    	return tcp;
    }

    private TCP() {
    	connections = new HashMap<>();
    	messagesToSend = new ArrayBlockingQueue<>(10);
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
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(GlobalSettings.localAddress());
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        GlobalSettings.FIXED_THREAD_POOL.submit(this::listenToSelector);
        GlobalSettings.FIXED_THREAD_POOL.submit(this::sendMessages);
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
        ByteBuffer messageBuffer = ByteBuffer.allocate(totalSizeBuffer.flip().getInt());
        socketChannel.read(messageBuffer);
        try {
            Message message = MessageDecoder.decodeMessage(messageBuffer);
				GlobalSettings.FLEX_THREAD_POOL.submit(() -> 
						MessageRouter.getRouter().deliverMessage(message));
        } catch(NullMessageException | InvalidMessageTypeException e) {
            // TODO
            System.exit(1);
        }
    }

    private void listenToSelector() {
        try {
            for(;;) {
                selector.select();
                Iterator<SelectionKey> keyIt = selector.selectedKeys().iterator();

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

    private void sendMessages() {
        try {
            for (;;) {
                // This thread waits for a Message, to send it, and blocks here, since its only responsibility
                // is to do this task
                Message messageToSend = messagesToSend.take();
                // If the Message has no destination, it can't be sent
                if(messageToSend.getDestination() == null || messageToSend.getDestination().isUnresolved())
                    throw new MessageHasNoDestinationException();
                SocketChannel socketChannel = connections.get(messageToSend.getDestination());
                // Something went wrong in the upper layers, probably
                if(socketChannel == null)
                    throw new ConnectionNotEstablishedException();
                ByteBuffer messageBuffer = messageToSend.encode();
                // First send the total size of the Message, so the receiving Node's TCP layer knows how much to read
                ByteBuffer totalSizeBuffer = ByteBuffer.allocate(Integer.BYTES);
                totalSizeBuffer.putInt(0, messageBuffer.capacity());
                socketChannel.write(totalSizeBuffer);
                // Then send the Message itself
                socketChannel.write(messageBuffer);
            }
        } catch(IOException | InterruptedException | MessageTooLargeException e) {
            // TODO also separate exception types
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

            channel = SocketChannel.open();
            channel.bind(GlobalSettings.localAddress());
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            // This is a non-blocking connect, and has to be, because this is the same thread that is reading
            // incoming messages from the network, and it cannot wait for this connection to be established
            channel.connect(remoteNodeAddress);
        } catch(IOException ioe) {
            // TODO
            System.exit(1);
        }
    }
    
    public void sendMessage(Message message)
    		throws IOException {
    	if(message.getDestination() == null)
    		throw new IOException();
    	try {
			messagesToSend.put(message);
		} catch (InterruptedException e) {
			// TODO
			System.exit(1);
		}
    }
}
