package network;

import common.Pair;
import exceptions.NotReadyForInitException;
import interfaces.Node;
import interfaces.Notifiable;
import interfaces.OnlineNotifiable;
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
    private Notifiable connector;

    // Map of existing connections, and the channel responsible for them
    private Map<InetSocketAddress, SocketChannel> connections;

    public TCP(InetSocketAddress address, short numTypes, int msgSize)
            throws IOException {
        super(address, numTypes, msgSize);

        connector = null;

        connections = new HashMap<>();
    }

    // Default values, can later be changed
    public TCP(InetSocketAddress address)
            throws IOException {

        this(address, (short) 30, Message.MSG_SIZE);
    }

    // Any problems here with replacing with no checks???
    @Override
    public void setConnector(Notifiable connector)
            throws IllegalArgumentException{

        if(connector == null)
            throw new IllegalArgumentException();

        this.connector = connector;
    }

    @Override
    public void init()
            throws NotReadyForInitException {

        if(msgSize == 0 || listeners == null || listeners.isEmpty() || connector == null)
            throw new NotReadyForInitException();

        triggerListenToSel();
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
                        channel.close();
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
                    for(OnlineNotifiable node : listeners.get(type)) {
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
