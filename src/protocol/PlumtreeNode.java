package protocol;

import common.MappedTimerManager;
import common.RandomChooser;
import common.TimerManager;
import exceptions.NotReadyForInitException;
import message.Message;
import message.plumtree.BodyMessage;
import message.plumtree.IHaveMessage;
import message.plumtree.PruneMessage;
import network.UDPInterface;
import test.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PlumtreeNode implements TreeBroadcastNode {

    // Names for the timers (threads)
    public final static String DELIVER = "D";

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;
    private int fanout;

    private Set<InetSocketAddress> lazyPeers;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private Set<IHaveMessage> ihaves;

    private UDPInterface udp;

    private RandomChooser<InetSocketAddress> random;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize, int fanout) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);
        this.fanout = fanout;

        lazyPeers = new HashSet<>(fanout - eagerPeerSetSize);

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        ihaves = new LinkedHashSet<>();

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

        for(InetSocketAddress peer : lazyPeers)
            try {
                udp.send(iHave.bytes(), peer);
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

    // TODO how to implement this???
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

        new Thread(this::deliver, DELIVER).start();
    }

    @Override
    public InetSocketAddress id() {
        return id;
    }

    @Override
    public void notifyMessage(ByteBuffer bytes) {
        short type = bytes.getShort();

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
        ihaves.add(IHaveMessage.parse(bytes));
    }

    private void handlePrune(ByteBuffer bytes) {
        PruneMessage msg = PruneMessage.parse(bytes);
        removeFromEager(msg.sender(), false);
    }

    @Override
    public boolean setUDP(UDPInterface udp) throws IllegalArgumentException {
        this.udp = udp;
        return true;
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
}
