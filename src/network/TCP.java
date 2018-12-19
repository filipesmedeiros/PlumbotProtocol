package network;

import exceptions.NotReadyForInitException;
import interfaces.Notifiable;
import interfaces.NetworkNotifiable;
import message.Message;
import notifications.MessageNotification;
import notifications.Notification;
import notifications.TCPConnectionNotification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.BufferUnderflowException;
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
            throws IOException {

        ServerSocketChannel ssChannel = (ServerSocketChannel) selectableChannel;

        SocketChannel channel = ssChannel.accept();

        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        channel.register(selector, SelectionKey.OP_READ);

        System.out.println(address + " is reading from " + channel.getRemoteAddress());
    }

    private void finishConnection(SelectionKey key, SelectableChannel selectableChannel)
            throws IOException {
        SocketChannel channel = (SocketChannel) selectableChannel;

        if(channel.finishConnect()) {
            key.interestOps(0);

            channel.register(selector, SelectionKey.OP_READ);

            System.out.println(address + " is reading (and " + channel.getLocalAddress() + " is sending id to) from " + channel.getRemoteAddress());

            // exchange "real" ids, so we store the id of the peer, and not the specific port (on the hash map)
            // this makes it easier for upper layers, because they don't have to know specific ports
            channel.write(idBuffer(true));
        }
    }

    private void read(SelectableChannel selectableChannel)
            throws IOException, InterruptedException {
        System.out.println("???");
        SocketChannel channel = (SocketChannel) selectableChannel;

        ByteBuffer buffer = ByteBuffer.allocate(msgSize);
        channel.read(buffer);

        buffer.flip();

        if(handleIdExchange(buffer, channel))
            return;

        short type = buffer.getShort(0);

        Notification messageNoti = new MessageNotification(buffer);
        for(NetworkNotifiable notifiable : listeners.get(type))
            notifiable.notify(messageNoti);
    }

    private boolean handleIdExchange(ByteBuffer buffer, SocketChannel channel)
            throws IOException, InterruptedException {

        System.out.println(address + " handling id");

        char whatExchange = buffer.getChar(0);

        // accepting == a, connecting == c
        if(whatExchange == 'a' || whatExchange == 'c') {
            buffer.getChar();

            InetSocketAddress id = parseAddress(buffer);
            connections.put(id, channel);

            Notification connectNoti;

            if(whatExchange == 'c') {
                connectNoti = new TCPConnectionNotification(id, true);

                channel.write(idBuffer(false));
            } else
                connectNoti = new TCPConnectionNotification(id, false);

            System.out.println(address + " exchanged ids with " + id);

            connector.notify(connectNoti);

            return true;
        }

        return false;
    }

    // Will not work in the real world(?)
    public static InetSocketAddress parseAddress(ByteBuffer bytes) {
        StringBuilder hostStr = new StringBuilder();

        while(true) {
            char c = bytes.getChar();
            if (c == ':')
                break;
            hostStr.append(c);
        }

        StringBuilder portStr = new StringBuilder();

        while(true) {
            char c = bytes.getChar();
            if (c == Message.EOS)
                break;
            portStr.append(c);
        }

        int port = Integer.parseInt(portStr.toString());

        // remove the slash from the string
        return new InetSocketAddress(hostStr.toString().substring(1), port);
    }

    private ByteBuffer idBuffer(boolean connecting) {
        String addrStr = address.toString();
        ByteBuffer id = ByteBuffer.allocate(2 + addrStr.length() * 2 + 2);

        if(connecting)
            id.putChar('c');
        else
            id.putChar('a');

        for(char c : addrStr.toCharArray())
            id.putChar(c);

        id.putChar(Message.EOS);

        return id.flip();
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
