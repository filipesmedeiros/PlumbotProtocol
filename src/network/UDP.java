package network;

import interfaces.Node;
import interfaces.OnlineNotifiable;
import message.Message;
import notifications.MessageNotification;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

public class UDP extends Network {

    final private DatagramChannel channel;

    public UDP(InetSocketAddress address, short numTypes, int msgSize)
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
        this(address, (short) 30, Message.MSG_SIZE);
    }

    @Override
    void listenToSelector()
            throws IOException, InterruptedException {

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for(SelectionKey key : keys) {
                if(key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(msgSize);
                    channel.receive(buffer);
                    buffer.flip();

                    short type = buffer.getShort(0);

                    MessageNotification notification = new MessageNotification(buffer);

                    for(OnlineNotifiable msgListener : listeners.get(type))
                        msgListener.notify(notification);
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
