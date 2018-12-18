package network;

import exceptions.NotReadyForInitException;
import interfaces.Notifiable;
import interfaces.NetworkNotifiable;
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

    private ServerSocketChannel server;

    // Node that handles connections (Xbot)
    private Notifiable connector;

    // Map of existing connections, and the channel responsible for them
    private Map<InetSocketAddress, SocketChannel> connections;

    public TCP(InetSocketAddress address, short numTypes, int msgSize)
            throws IOException {
        super(address, numTypes, msgSize);

        connector = null;

        connections = new HashMap<>();

        server = null;
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
    public void initialize()
            throws NotReadyForInitException, IOException {

        if(msgSize == 0 || listeners == null || listeners.isEmpty() || connector == null)
            throw new NotReadyForInitException();

        server = ServerSocketChannel.open();
        server.bind(address);
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

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
        channel.bind(new InetSocketAddress(address.getAddress(), 0));
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        channel.register(selector, SelectionKey.OP_CONNECT);

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

    @SuppressWarnings("all")
    @Override
    void listenToSelector()
            throws IOException, InterruptedException {

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIt = keys.iterator();

            while(keyIt.hasNext()) {
                SelectionKey key = keyIt.next();
                SelectableChannel selectableChannel = key.channel();

                if(key.isAcceptable())
                    accept(selectableChannel);
                else if(key.isConnectable())
                    finishConnection(key, selectableChannel);
                else if(key.isReadable())
                    read(selectableChannel);

                keyIt.remove();
            }
        }
    }

    private void accept(SelectableChannel selectableChannel)
            throws IOException, InterruptedException {

        ServerSocketChannel ssChannel = (ServerSocketChannel) selectableChannel;

        SocketChannel channel = ssChannel.accept();

        InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();

        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        channel.register(selector, SelectionKey.OP_READ, remote);

        connections.put(remote, channel);
        System.out.println(address + " added " + remote);

        Notification connectNoti = new TCPConnectionNotification(remote, true);
        connector.notify(connectNoti);
    }

    private void finishConnection(SelectionKey key, SelectableChannel selectableChannel)
            throws IOException, InterruptedException {
        SocketChannel channel = (SocketChannel) selectableChannel;

        if(channel.finishConnect()) {
            key.interestOps(0);

            channel.register(selector, SelectionKey.OP_READ);

            InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();
            connections.put(remote, channel);

            Notification connectNoti = new TCPConnectionNotification(remote, false);
            connector.notify(connectNoti);
        }
    }

    private void read(SelectableChannel selectableChannel)
            throws IOException, InterruptedException {
        SocketChannel channel = (SocketChannel) selectableChannel;

        ByteBuffer buffer = ByteBuffer.allocate(msgSize);
        channel.read(buffer);
        buffer.flip();

        short type = buffer.getShort(0);

        InetSocketAddress remote = (InetSocketAddress) channel.getRemoteAddress();
        Notification messageNoti = new MessageNotification(buffer, remote.getPort());
        for(NetworkNotifiable notifiable : listeners.get(type))
            notifiable.notify(messageNoti);
    }

    @SuppressWarnings("all")
    @Override
    void fulfill() {

        // System.out.println("Fulfilling message " + request.type());

        while(true) {
            try {
                MessageRequest request = requests.take();

                InetSocketAddress to = request.to();
                SocketChannel channel = connections.get(to);

                if(channel == null)
                    System.out.println(address + " type " + request.type() + " to " + request.to());

                channel.write(request.message());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }
}
