package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.Node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class Network implements NetworkInterface {

    final Selector selector;
    final InetSocketAddress address;

    int msgSize;
    List<List<Node>> listeners;
    BlockingQueue<MessageRequest> requests;

    public Network(InetSocketAddress address, int numTypes, int msgSize)
            throws IOException {
        this.address = address;

        selector = Selector.open();

        requests = new ArrayBlockingQueue<>(10);

        listeners = new ArrayList<>(numTypes);
        for(int i = 0; i < numTypes; i++)
            listeners.add(new LinkedList<>());

        this.msgSize = msgSize;
    }

    @Override
    public void send(ByteBuffer bytes, InetSocketAddress to)
            throws InterruptedException, IllegalArgumentException {

        if(bytes.capacity() > msgSize)
            throw new IllegalArgumentException();

        requests.put(new GeneralMessageRequest(bytes, to));
    }

    @Override
    public NetworkInterface setMsgSize(int size) {
        msgSize = size;
        return this;
    }

    @Override
    public NetworkInterface addMessageListener(Node listener, List<Short> msgTypes) {
        for(Short index : msgTypes)
            listeners.get(index).add(listener);

        return this;
    }

    @Override
    public NetworkInterface addMessageListeners(Map<Node, List<Short>> listeners) {
        for(Map.Entry<Node, List<Short>> entry : listeners.entrySet())
            addMessageListener(entry.getKey(), entry.getValue());

        return this;
    }

    @Override
    public NetworkInterface setRequestQueueSize(int size) throws CantResizeQueueException {
        if(size < requests.size())
            throw new CantResizeQueueException();

        BlockingQueue<MessageRequest> newQ = new ArrayBlockingQueue<>(size);
        newQ.addAll(requests);
        requests = newQ;

        return this;
    }

    @Override
    public void init()
            throws NotReadyForInitException {

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

    abstract void receive() throws IOException;

    abstract void fulfill();
}
