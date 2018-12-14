package network;

import interfaces.Node;
import message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

public class UDP extends Network {

    final private DatagramChannel channel;

    public UDP(InetSocketAddress address, Node node, short numTypes, int msgSize)
            throws IOException {
        super(address, node, numTypes, msgSize);

        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);

        channel.register(selector, SelectionKey.OP_READ);
    }

    // Default values, can later be changed
    public UDP(InetSocketAddress address, Node node)
            throws IOException {
        this(address, node, (short) 30, Message.MSG_SIZE);
    }

    @Override
    void listenToSelector()
            throws IOException {

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for(SelectionKey key : keys) {
                if(key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(msgSize);
                    channel.receive(buffer);
                    buffer.flip();

                    short type = buffer.getShort(0);

                    for(Node msgListener : listeners.get(type))
                        msgListener.notify(buffer);
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

                channel.send(request.message(), request.to());
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }
}
