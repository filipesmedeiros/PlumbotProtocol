package network;

import exceptions.CantResizeQueueException;
import exceptions.NotReadyForInitException;
import interfaces.NetworkNotifiable;

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
    Map<Short, List<NetworkNotifiable>> listeners;
    BlockingQueue<MessageRequest> requests;

    public Network(InetSocketAddress address, short numTypes, int msgSize)
            throws IOException {
        this.address = address;

        selector = Selector.open();

        requests = new ArrayBlockingQueue<>(10);

        listeners = new HashMap<>(numTypes * 4 / 3);
        for(short i = 0; i < numTypes; i++)
            listeners.put(i, new LinkedList<>());

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
    public NetworkInterface addMessageListener(NetworkNotifiable listener, List<Short> msgTypes) {
        for(Short index : msgTypes)
            listeners.get(index).add(listener);

        return this;
    }

    @Override
    public NetworkInterface addMessageListeners(Map<NetworkNotifiable, List<Short>> listeners) {
        for(Map.Entry<NetworkNotifiable, List<Short>> entry : listeners.entrySet())
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
    public void initialize()
            throws NotReadyForInitException, IOException {

        if(msgSize == 0 || listeners == null || listeners.isEmpty())
            throw new NotReadyForInitException();

        triggerListenToSel();
    }

    void triggerListenToSel() {
        new Thread(() -> {
            try {
                listenToSelector();
            } catch (IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        }, LISTEN_TO_SELECTOR_THREAD + address).start();

        fulfill();
    }

    abstract void listenToSelector() throws IOException, InterruptedException;

    abstract void fulfill();
}
