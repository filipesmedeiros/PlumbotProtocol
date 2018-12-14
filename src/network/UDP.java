package network;

import message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

public class UDP extends Network {

    final private DatagramChannel channel;

    public UDP(InetSocketAddress address, int numTypes, int msgSize)
            throws IOException {
        super(address, numTypes, msgSize);

        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);

        channel.register(selector, SelectionKey.OP_READ);
    }

    // Default values, can later be changed
    public UDP(InetSocketAddress address)
            throws IOException {

        this(address, 30, Message.MSG_SIZE);
    }

    @Override
    void receive()
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

                    for(MessageListener msgListener : listeners.get(type))
                        msgListener.notifyMessage(buffer);
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
