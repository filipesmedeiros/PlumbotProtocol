package protocol;

import common.MappedTimerManager;
import common.RandomChooser;
import common.TimerManager;
import exceptions.NotReadyForInitException;
import message.Message;
import message.plumtree.*;
import network.UDPInterface;
import test.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PlumtreeNode implements TreeBroadcastNode {

    // Names for the timers (threads)
    public final static String DELIVER = "D";
    // public final static String REQUEST = "R";
    public final static String GRAFT = "G";

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;
    // private int fanout;

    private Set<InetSocketAddress> lazyPeers;

    private Map<Integer, Long> receivedHashes;
    private long repeatMessageTimeout;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private BlockingQueue<IHaveMessage> missing;
    private Map<Integer, CountedMessage> msgsToBeSent;

    private BlockingQueue<IHaveMessage> lazyQueue;

    private long iHaveTimeout;
    private double ihtoMultiplier;

    private TimerManager timerManager;

    private UDPInterface udp;

    private RandomChooser<InetSocketAddress> random;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize,
                        int fanout, long repeatMessageTimeout, long iHaveTimeout,
                        long lazyGatherWait) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);
        // this.fanout = fanout;

        lazyPeers = new HashSet<>(fanout - eagerPeerSetSize);

        receivedHashes = new HashMap<>();
        this.repeatMessageTimeout = repeatMessageTimeout;

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        missing = new ArrayBlockingQueue<>(10);
        msgsToBeSent = new HashMap<>();

        lazyQueue = new ArrayBlockingQueue<>(10);

        this.iHaveTimeout = iHaveTimeout;
        ihtoMultiplier = 0.6;

        timerManager = new MappedTimerManager();

        udp = null;

        random = new RandomChooser<>();
    }

    @Override
    public void broadcast(ByteBuffer bytes) throws IllegalArgumentException {
        eagerPushMessage(bytes);

        int hash = hashMessage(bytes);
        IHaveMessage iHave = new IHaveMessage(id, hash, (short) 1, id);

        lazyQueue.add(iHave);

        msgsToBeSent.put(iHave.hash(), new CountedMessage(bytes, new HashSet<>()));
    }

    private void eagerPushMessage(ByteBuffer bytes) {
        for(InetSocketAddress peer : eagerPeers)
            try {
                BodyMessage msg = new BodyMessage(id, bytes, bytes.limit(), (short) 1);
                udp.send(msg.next(id).bytes(), peer);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    private void lazyPushMessage() {
        while(!lazyQueue.isEmpty())
            try {
                IHaveMessage iHave = lazyQueue.take();

                CountedMessage counted = msgsToBeSent.get(iHave.hash());
                for(InetSocketAddress peer : lazyPeers) {
                    udp.send(iHave.bytes(), peer);
                    counted.addPeer(peer);
                }
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
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
        for(Map.Entry<Integer, CountedMessage> msg : msgsToBeSent.entrySet())
            if(msg.getValue().peers.remove(peer))
                msgsToBeSent.remove(msg.getKey());

        for(IHaveMessage msg : missing)
            if(msg.sender().equals(peer))
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

    // IS THIS BLOCKING THE EXECUTION??? (CAN GO BACK TO UDP)
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
            case GraftMessage.TYPE:
                handleGraft(bytes);
                break;

            default:
                System.out.println("??? Unrecognized message");
                break;
        }
    }

    private void handleBody(ByteBuffer bytes) {
        try {
            BodyMessage msg = BodyMessage.parse(bytes);

            int hash = hashMessage(bytes);

            Long firstReceive = receivedHashes.get(hash);
            if(firstReceive != null
                    && System.currentTimeMillis() - firstReceive < repeatMessageTimeout)
                removeFromEager(msg.sender(), true);
            else {
                receivedHashes.put(hash, System.currentTimeMillis());
                messages.put(msg);

                for(InetSocketAddress peer : eagerPeers)
                    if(peer.equals(msg.sender()))
                        try {
                            udp.send(msg.next(id).bytes(), peer);
                        } catch(IOException e) {
                            // TODO
                            e.printStackTrace();
                        }
            }

            for(IHaveMessage missMsg : missing)
                if(missMsg.hash() == hash) {
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

    private void handleRequest(ByteBuffer bytes) {
        RequestMessage msg = RequestMessage.parse(bytes);

        sendBodyFromHash(msg.sender(), msg.hash());
    }

    private void handleGraft(ByteBuffer bytes) {
        GraftMessage msg = GraftMessage.parse(bytes);

        moveToEager(msg.sender());

        if(msg.send())
            sendBodyFromHash(msg.sender(), msg.hash());
    }

    private void sendBodyFromHash(InetSocketAddress sender, int hash) {
        CountedMessage counted = msgsToBeSent.get(hash);
        if(counted == null)
            return;

        Message bodyMessage = new BodyMessage(id, counted.bytes, counted.bytes.limit(), (short) 1);

        try {
            udp.send(bodyMessage.bytes(), sender);
        } catch(IOException | InterruptedException e) {
            // TODO
            e.printStackTrace();
        }

        if(counted.removePeer(sender))
            msgsToBeSent.remove(hash);
    }

    private void triggerGraft(Object msgO) {
        IHaveMessage msg = (IHaveMessage) msgO;
        int hash = msg.hash();

        if(receivedHashes.containsKey(hash))
            return;

        for(IHaveMessage missMsg : missing)
            if(missMsg.hash() == msg.hash())
                try {
                    Message graftMsg = new GraftMessage(id, hash, true);
                    udp.send(graftMsg.bytes(), missMsg.sender());

                    timerManager.addAction(GRAFT, this::triggerGraft, msg, (long)(iHaveTimeout * ihtoMultiplier));
                } catch(InterruptedException | IOException e) {
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

                // I think this makes sense here, but does it?
                timerManager.addAction(GRAFT, this::triggerGraft, msg, iHaveTimeout);
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

    private void moveToEager(InetSocketAddress peer) {
        if(lazyPeers.contains(peer)) {
            lazyPeers.remove(peer);
            eagerPeers.add(peer);
        } else
            System.out.println("??? Not in lazy");
    }

    @Override
    public boolean canRemove(InetSocketAddress peer) throws IllegalArgumentException {
        return !eagerPeers.contains(peer);
    }

    private static class CountedMessage {
        private ByteBuffer bytes;
        private Set<InetSocketAddress> peers;

        private CountedMessage(ByteBuffer bytes, Set<InetSocketAddress> peers) {
            this.bytes = bytes;
            this.peers = peers;
        }

        boolean removePeer(InetSocketAddress peer) {
            peers.remove(peer);
            return peers.isEmpty();
        }

        void addPeer(InetSocketAddress peer) {
            peers.add(peer);
        }
    }
}
