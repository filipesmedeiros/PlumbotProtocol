package network;

import common.Pair;
import interfaces.Node;
import interfaces.Notifiable;
import message.Message;
import notifications.MessageNotification;
import notifications.Notification;
import notifications.TCPConnectionNotification;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class TCP extends Network implements PersistantNetwork {

    // Node that handles connections (Xbot)
    final Notifiable connector;

    // Map of existing connections, and the channel responsible for them
    private Map<InetSocketAddress, SocketChannel> connections;

    public TCP(InetSocketAddress address, Notifiable connector, short numTypes, int msgSize)
            throws IOException {
        super(address, numTypes, msgSize);

        this.connector = connector;

        connections = new HashMap<>();
    }

    // Default values, can later be changed
    public TCP(InetSocketAddress address, Node node)
            throws IOException {

        this(address, node, (short) 30, Message.MSG_SIZE);
    }

    // Returns the channel if we were already connected
    // and returns null if connection was attempted
    @Override
    public SocketChannel connect(InetSocketAddress remote)
            throws IOException {
        SocketChannel channel = connections.get(remote);
        if(channel != null)
            return channel;

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        channel.register(selector, SelectionKey.OP_CONNECT, remote);

        try {
            channel.connect(remote);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean disconnect(InetSocketAddress remote)
            throws IOException {

        SocketChannel channel = connections.get(remote);
        if(channel == null)
            return false;

        if(!channel.isConnected())
            return false;

        channel.close();

        return true;
    }

    @Override
    void listenToSelector()
            throws IOException, InterruptedException {

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for(SelectionKey key : keys) {
                SelectableChannel selectableChannel = key.channel();
                if(!(selectableChannel instanceof SocketChannel))
                    continue;

                SocketChannel channel = (SocketChannel) selectableChannel;

                if(key.isConnectable()) {
                    if(channel.finishConnect()) {
                        key.cancel();
                        channel.register(selector, SelectionKey.OP_READ);

                        InetSocketAddress remote = (InetSocketAddress) key.attachment();
                        connections.put(remote, channel);

                        Notification connectNoti = new TCPConnectionNotification(remote);
                        connector.notify(connectNoti);
                    }
                } else if(key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(msgSize);
                    channel.read(buffer);
                    buffer.flip();

                    short type = buffer.getShort(0);

                    Notification messageNoti = new MessageNotification(buffer);
                    for(Node node : listeners.get(type)) {
                        node.notify(messageNoti);
                    }
                }
            }
        }
    }

    @Override
    void fulfill() {

        // System.out.println("Fulfilling message " + request.type());

        while(true) {
            MessageRequest request;
            try {
                request = requests.take();

                InetSocketAddress to = request.to();
                SocketChannel channel = connections.get(to);

                channel.write(request.message());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }
}
