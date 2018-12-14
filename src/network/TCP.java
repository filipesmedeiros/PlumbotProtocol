package network;

import interfaces.Node;
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

public class TCP extends Network {

    private Map<InetSocketAddress, SocketChannel> connections;

    public TCP(InetSocketAddress address, int numTypes, int msgSize)
            throws IOException {
        super(address, numTypes, msgSize);

        connections = new HashMap<>();
    }

    // Default values, can later be changed
    public TCP(InetSocketAddress address)
            throws IOException {

        this(address, 30, Message.MSG_SIZE);
    }

    // Returns the channel if we were already connected
    // and returns null if connection was attempted
    public SocketChannel connect(InetSocketAddress peer)
            throws IOException {
        SocketChannel channel = connections.get(peer);
        if(channel != null)
            return channel;

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        channel.register(selector, SelectionKey.OP_CONNECT, peer);

        try {
            channel.connect(peer);
        } catch(IOException e) {
            // TODO
            e.printStackTrace();
        }

        return null;
    }

    @Override
    void receive()
            throws IOException {

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for(SelectionKey key : keys) {
                SelectableChannel sChannel = key.channel();
                if(!(sChannel instanceof SocketChannel))
                    continue;

                SocketChannel channel = (SocketChannel) sChannel;

                if(key.isConnectable()) {
                    InetSocketAddress remote = (InetSocketAddress) key.attachment();
                    connections.put(remote, channel);

                    Notification connectNoti = new TCPConnectionNotification((InetSocketAddress) key.attachment());
                    for(Node node : listeners.get(type)) {
                        node.notify(messageNoti);
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

                channel.write(request.message());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }
}
