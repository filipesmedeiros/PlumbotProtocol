package protocol;

import common.MappedTimerManager;
import common.RandomChooser;
import common.TimerManager;
import exceptions.NotReadyForInitException;
import message.Message;
import message.plumtree.*;
import network.NetworkInterface;
import network.PersistantNetwork;
import notifications.*;
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

    private BlockingQueue<Notification> notifications;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private BlockingQueue<IHaveMessage> missing;
    private Map<Integer, CountedMessage> msgsToBeSent;

    private BlockingQueue<IHaveMessage> lazyQueue;

    private long iHaveTimeout;
    private double ihtoMultiplier;

    private TimerManager timerManager;

    private NetworkInterface tcp;

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

        notifications = new ArrayBlockingQueue<>(10);

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        missing = new ArrayBlockingQueue<>(10);
        msgsToBeSent = new HashMap<>();

        lazyQueue = new ArrayBlockingQueue<>(10);

        this.iHaveTimeout = iHaveTimeout;
        ihtoMultiplier = 0.6;

        timerManager = new MappedTimerManager();

        tcp = null;

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

    private void broadcast(BodyMessage message) {
        broadcast(message.body());
    }

    private void eagerPushMessage(ByteBuffer bytes) {
        System.out.println(eagerPeers.size());
        for(InetSocketAddress peer : eagerPeers)
            try {
                BodyMessage msg = new BodyMessage(id, bytes, bytes.limit(), (short) 1);
                tcp.send(msg.next(id).bytes(), peer);
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
                    tcp.send(iHave.bytes(), peer);
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

    // half the && are for confirmation of errors (cuz they should be symmetrical)
    @Override
    public boolean canOptimize(InetSocketAddress peer1, InetSocketAddress peer2) {
        if(peer1 != null && peer2 != null)
            return !(id.equals(peer1) && eagerPeers.contains(peer2)
                    || id.equals(peer2) && eagerPeers.contains(peer1));

        return true;
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
        if(tcp == null)
            throw new NotReadyForInitException();

        new Thread(this::deliver, DELIVER).start();

        processNotification();
    }

    @Override
    public InetSocketAddress id() {
        return id;
    }

    @Override
    public void notify(Notification notification) {
        notifications.add(notification);
    }

    private boolean hashIsOutdated(long firstReceive) {
        return System.currentTimeMillis() - firstReceive > repeatMessageTimeout;
    }

    private void handleBody(ByteBuffer bytes) {
        try {
            BodyMessage msg = BodyMessage.parse(bytes);

            int hash = hashMessage(bytes);

            Long firstReceive = receivedHashes.get(hash);
            if(firstReceive != null && !hashIsOutdated(firstReceive))
                removeFromEager(msg.sender(), true);
            else {
                receivedHashes.put(hash, System.currentTimeMillis());
                messages.put(msg);

                broadcast(msg);
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

            if(!haveMessage(msg.hash())) {
                Message request = new RequestMessage(id, msg.hash());

                tcp.send(request.bytes(), msg.sender());

                // I think this makes sense here, but does it?
                timerManager.addAction(GRAFT, this::triggerGraft, msg, iHaveTimeout);
            }
        } catch(IOException | InterruptedException e) {
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
            tcp.send(bodyMessage.bytes(), sender);
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
                    tcp.send(graftMsg.bytes(), missMsg.sender());

                    timerManager.addAction(GRAFT, this::triggerGraft, msg, (long)(iHaveTimeout * ihtoMultiplier));
                } catch(InterruptedException | IOException e) {
                    // TODO
                    e.printStackTrace();
                }
    }

    // TODO does this screw something? something should be checked?
    @Override
    public boolean setNetwork(NetworkInterface tcp)
            throws IllegalArgumentException {

        if(!(tcp instanceof PersistantNetwork))
            throw new IllegalArgumentException();

        this.tcp = tcp;
        return true;
    }

    @SuppressWarnings("all")
    private void handleMessage(Notification notification) {
        if(!(notification instanceof MessageNotification)) {
            System.out.println("??? Wrong message notification");
            return;
        }

        MessageNotification msgNoti = (MessageNotification) notification;

        short type = msgNoti.type();

        switch(type) {
            case BodyMessage.TYPE:
                handleBody(msgNoti.message());
                break;
            case GraftMessage.TYPE:
                handleGraft(msgNoti.message());
                break;
            case RequestMessage.TYPE:
                handleRequest(msgNoti.message());
                break;
            case PruneMessage.TYPE:
                handlePrune(msgNoti.message());
                break;
            case IHaveMessage.TYPE:
                handleIHave(msgNoti.message());
                break;

            default:
                System.out.println("??? unrecognized message");
                break;
        }
    }

    @SuppressWarnings("all")
    private void processNotification() {
        while(true)
            try {
                Notification notification = notifications.take();

                short type = notification.type();

                switch (type) {
                    case MessageNotification.TYPE:
                        handleMessage(notification);
                        break;

                    default:
                        System.out.println("??? Wrong notification");
                        break;
                }
            } catch(InterruptedException e) {
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
                tcp.send(pruneMsg.bytes(), peer);
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
