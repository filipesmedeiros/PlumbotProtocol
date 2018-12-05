package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.MessageListener;
import message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UDP implements UDPInterface {

    final private Selector selector;
    final private DatagramChannel channel;
    final private InetSocketAddress address;

    private int msgSize;
    private List<List<MessageListener>> listeners;
    private BlockingQueue<MessageRequest> requests;

    public UDP(InetSocketAddress address, int numTypes, int msgSize)
            throws IOException {

        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(address);

        this.address = address;

        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);

        requests = new ArrayBlockingQueue<>(10);

        listeners = new ArrayList<>(numTypes);
        for(int i = 0; i < numTypes; i++)
            listeners.add(new LinkedList<>());

        this.msgSize = msgSize;
    }

    // Default values, can later be changed
    public UDP(InetSocketAddress address)
            throws IOException, InterruptedException {

        this(address, 30, Message.MSG_SIZE);
    }

    @Override
    public void send(ByteBuffer bytes, InetSocketAddress to)
            throws InterruptedException, IllegalArgumentException {

        if(bytes.capacity() > msgSize)
            throw new IllegalArgumentException();

        requests.put(new GeneralMessageRequest(bytes, to));
    }

    @Override
    public UDPInterface setMsgSize(int size) {
        msgSize = size;
        return this;
    }

    @Override
    public UDPInterface addMessageListener(MessageListener listener, List<Short> msgTypes) {
        for(Short index : msgTypes)
            listeners.get(index).add(listener);

        return this;
    }

    @Override
    public UDPInterface addMessageListeners(Map<MessageListener, List<Short>> listeners) {
        for(Map.Entry<MessageListener, List<Short>> entry : listeners.entrySet())
            addMessageListener(entry.getKey(), entry.getValue());

        return this;
    }

    @Override
    public UDPInterface setRequestQueueSize(int size) throws CantResizeQueueException {
        if(size < requests.size())
            throw new CantResizeQueueException();

        BlockingQueue<MessageRequest> newQ = new ArrayBlockingQueue<>(size);
        newQ.addAll(requests);
        requests = newQ;

        return this;
    }

    @Override
    public void init()
            throws IOException, NotReadyForInitException {

        if(msgSize == 0 || listeners == null || listeners.isEmpty())
            throw new NotReadyForInitException();

        new Thread(() -> {
            try {
                receive();
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
            }
        }, RECEIVE_THREAD + address).start();

        fulfill();
    }

    private void receive()
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

    private void fulfill() {

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
