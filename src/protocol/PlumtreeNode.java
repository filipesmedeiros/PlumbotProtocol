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
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

public class PlumtreeNode implements TreeBroadcastNode {

    // Names for the timers (threads)
    public final static String DELIVER = "D";
    // public final static String REQUEST = "R";

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;
    // private int fanout;

    private Set<InetSocketAddress> lazyPeers;

    private Map<Integer, Long> receivedHashes;
    private long repeatMessageTimeout;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private BlockingQueue<Entry<InetSocketAddress, Integer>> missing;
    private Map<Integer, CountedMessage> toBeSent;
    private long iHaveTimeout;

    private UDPInterface udp;

    private RandomChooser<InetSocketAddress> random;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize,
                        int fanout, long repeatMessageTimeout, long iHaveTimeout) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);
        // this.fanout = fanout;

        lazyPeers = new HashSet<>(fanout - eagerPeerSetSize);

        receivedHashes = new HashMap<>();
        this.repeatMessageTimeout = repeatMessageTimeout;

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        missing = new ArrayBlockingQueue<>(10);
        toBeSent = new HashMap<>();
        this.iHaveTimeout = iHaveTimeout;

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

        List<InetSocketAddress> sentTo = new LinkedList<>();
        for(InetSocketAddress peer : lazyPeers)
            try {
                udp.send(iHave.bytes(), peer);

                sentTo.add(peer);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }

        toBeSent.put(hash, new CountedMessage(bytes, sentTo));
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
    public void addApplication(Application app)
            throws IllegalArgumentException {

        if(app == null)
            throw new IllegalArgumentException();

        applications.add(app);
    }

    @Override
    public void addApplications(Set<Application> apps)
            throws IllegalArgumentException {

        if(apps == null || apps.isEmpty())
            throw new IllegalArgumentException();

        applications.addAll(apps);

    }

    @Override
    public void neighbourUp(InetSocketAddress peer) {
        eagerPeers.add(peer);
    }

    @Override
    public void neighbourDown(InetSocketAddress peer) {
        for(Map.Entry<Integer, CountedMessage> msg : toBeSent.entrySet())
            if(msg.getValue().peers.remove(peer))
                toBeSent.remove(msg.getKey());

        for(Entry msg : missing)
            if(msg.getKey().equals(peer))
                missing.remove(msg);

        eagerPeers.remove(peer);
        lazyPeers.remove(peer);
    }

    @Override
    public void initialize()
            throws NotReadyForInitException {
        if(udp == null)
            throw new NotReadyForInitException();

        new Thread(this::deliver, DELIVER).start();

        requestMessage();
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
    }

    private void handleBody(ByteBuffer bytes) {
        try {
            BodyMessage msg = BodyMessage.parse(bytes);

            messages.put(msg);

            int hash = hashMessage(bytes);

            Long firstReceive = receivedHashes.get(hash);
            if(firstReceive != null
                    && System.currentTimeMillis() - firstReceive < repeatMessageTimeout)
                removeFromEager(msg.sender(), true);
            else
                receivedHashes.put(hash, System.currentTimeMillis());

            for(Entry<InetSocketAddress, Integer> missMsg : missing)
                if(missMsg.getValue() == hash) {
                    missing.remove(missMsg);
                    break;
                }
        } catch(InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handleIHave(ByteBuffer bytes) {
        try {
            IHaveMessage msg = IHaveMessage.parse(bytes);

            int hash = msg.hash();
            if(!haveMessage(hash))
                missing.put(new SimpleEntry<>(msg.sender(), hash));


        } catch(InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void handlePrune(ByteBuffer bytes) {
        PruneMessage msg = PruneMessage.parse(bytes);
        removeFromEager(msg.sender(), false);
    }

    private void handleRequest(ByteBuffer bytes) {
        RequestMessage msg = RequestMessage.parse(bytes);
        try {
            CountedMessage counted = toBeSent.get(msg.hash());
            if(counted == null)
                return;

            Message bodyMessage = new BodyMessage(id, counted.bytes, counted.bytes.limit());

            udp.send(bodyMessage.bytes(), msg.sender);

            if(counted.removePeer(msg.sender))
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
                Entry<InetSocketAddress, Integer> msg = missing.take();

                Message request = new RequestMessage(id, msg.getValue());

                udp.send(request.bytes(), msg.getKey());
            } catch(InterruptedException | IOException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private boolean haveMessage(int hash) {
        for(Entry<InetSocketAddress, Integer> msg : missing)
            if(msg.getValue() == hash)
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

    @Override
    public boolean canRemove(InetSocketAddress peer) throws IllegalArgumentException {
        return !eagerPeers.contains(peer);
    }

    private static class CountedMessage {
        private ByteBuffer bytes;
        private List<InetSocketAddress> peers;

        private CountedMessage(ByteBuffer bytes, List<InetSocketAddress> peers) {
            this.bytes = bytes;
            this.peers = peers;
        }

        boolean removePeer(InetSocketAddress peer) {
            peers.remove(peer);
            return peers.isEmpty();
        }
    }
}
