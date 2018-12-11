package protocol;

import common.RandomChooser;
import exceptions.NotReadyForInitException;
import message.Message;
import message.plumtree.BodyMessage;
import message.plumtree.IHaveMessage;
import message.plumtree.PruneMessage;
import message.plumtree.RequestMessage;
import network.UDPInterface;
import test.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PlumtreeNode implements TreeBroadcastNode {

    // Names for the timers (threads)
    public final static String DELIVER = "D";
    public final static String REQUEST = "R";

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;
    private int fanout;

    private Set<InetSocketAddress> lazyPeers;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private BlockingQueue<IHaveMessage> missing;
    private Map<Integer, CountedMessage> toBeSent;

    private UDPInterface udp;

    private RandomChooser<InetSocketAddress> random;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize, int fanout) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);
        this.fanout = fanout;

        lazyPeers = new HashSet<>(fanout - eagerPeerSetSize);

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        missing = new ArrayBlockingQueue<>(10);
        toBeSent = new HashMap<>();

        udp = null;

        random = new RandomChooser<>();
    }

    @Override
    public void broadcast(ByteBuffer bytes) throws IllegalArgumentException {
        eagerPushMessage(bytes);
        lazyPushMessage(bytes);
    }

    private void eagerPushMessage(ByteBuffer bytes) {
        for(InetSocketAddress peer : eagerPeers)
            try {
                udp.send(bytes, peer);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private void lazyPushMessage(ByteBuffer bytes) {
        int hash = hashMessage(bytes);
        Message iHave = new IHaveMessage(id, hash);

        int counter = 0;
        for(InetSocketAddress peer : lazyPeers)
            try {
                udp.send(iHave.bytes(), peer);

                counter++;
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }

        toBeSent.put(hash, new CountedMessage(bytes, counter));
    }

    // Only here in case it gets more complex in the future
    private int hashMessage(ByteBuffer bytes) {
        return bytes.hashCode();
    }

    @Override
    public int eagerPeerSetSize(int size) throws IllegalArgumentException {
        int count = 0;

        while(eagerPeers.size() > size) {
            removeFromEager(random.fromSet(eagerPeers), true);
            count++;
        }

        return count;
    }

    // TODO how to implement this??? many ways
    @Override
    public int lazyPeerSetSize(int size) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void neighbourUp(InetSocketAddress peer) {

    }

    @Override
    public void neighbourDown(InetSocketAddress peer) {

    }

    @Override
    public void initialize()
            throws NotReadyForInitException {
        if(udp == null)
            throw new NotReadyForInitException();

        new Thread(this::deliver, DELIVER).start();

        new Thread(this::requestMessage, REQUEST).start();
    }

    @Override
    public InetSocketAddress id() {
        return id;
    }

    @Override
    public void notifyMessage(ByteBuffer bytes) {
        short type = bytes.getShort();

        switch(type) {
            case BodyMessage.TYPE:
                handleBody(bytes);
                break;
            case IHaveMessage.TYPE:
                handleIHave(bytes);
                break;
            case PruneMessage.TYPE:
                handlePrune(bytes);
                break;
            case RequestMessage.TYPE:
                handleRequest(bytes);
                break;

            default:
                System.out.println("???");
                break;
        }
        if(type == BodyMessage.TYPE)
            handleBody(bytes);
        else if(type == IHaveMessage.TYPE)
            handleIHave(bytes);
        else if(type == PruneMessage.TYPE)
            handlePrune(bytes);
    }

    private void handleBody(ByteBuffer bytes) {
        try {
            BodyMessage msg = BodyMessage.parse(bytes);

            messages.put(msg);
        } catch(InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleIHave(ByteBuffer bytes) {
        try {
            IHaveMessage msg = IHaveMessage.parse(bytes);

            if(!haveMessage(msg.hash()))
                missing.put(msg);
        } catch(InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handlePrune(ByteBuffer bytes) {
        PruneMessage msg = PruneMessage.parse(bytes);
        removeFromEager(msg.sender(), false);
    }

    // Eventually should check if requester is real (IP address
    // is one to which we sent the IHave)
    private void handleRequest(ByteBuffer bytes) {
        RequestMessage msg = RequestMessage.parse(bytes);
        try {
            CountedMessage counted = toBeSent.get(msg.hash());
            if(counted == null)
                return;

            Message bodyMessage = new BodyMessage(id, counted.bytes, counted.bytes.limit());

            udp.send(bodyMessage.bytes(), msg.sender);

            if(counted.decCounter())
                toBeSent.remove(msg.hash());
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public boolean setUDP(UDPInterface udp) throws IllegalArgumentException {
        this.udp = udp;
        return true;
    }

    private void requestMessage() {
        while(true)
            try {
                IHaveMessage msg = missing.take();

                Message request = new RequestMessage(id, msg.hash());

                udp.send(request.bytes(), msg.sender());
            } catch(InterruptedException | IOException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private boolean haveMessage(int hash) {
        for(IHaveMessage msg : missing)
            if(msg.hash() == hash)
                return true;

        return false;
    }

    private void deliver() {
        while(true)
            try {
                BodyMessage msg = messages.take();

                for(Application app : applications)
                    app.deliver(msg);
            } catch(InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private void removeFromEager(InetSocketAddress peer, boolean prune) {
        eagerPeers.remove(peer);

        lazyPeers.add(peer);

        if(prune)
            try {
                Message pruneMsg = new PruneMessage(id);
                udp.send(pruneMsg.bytes(), peer);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private static class CountedMessage {
        private ByteBuffer bytes;
        private Integer counter;

        private CountedMessage(ByteBuffer bytes, Integer counter) {
            this.bytes = bytes;
            this.counter = counter;
        }

        // returns true if counter is 0, false otherwise
        public boolean decCounter() {
            return --counter == 0;
        }
    }
}
