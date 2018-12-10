package protocol;

import message.plumtree.BodyMessage;
import message.plumtree.IHaveMessage;
import network.UDPInterface;
import test.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PlumtreeNode implements TreeBroadcastNode {

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;
    private int fanout;

    private BlockingQueue<BodyMessage> messages;
    private Set<Application> applications;

    private Set<IHaveMessage> ihaves;

    private UDPInterface udp;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize, int fanout) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);
        this.fanout = fanout;

        messages = new ArrayBlockingQueue<>(10);
        applications = new HashSet<>();

        ihaves = new LinkedHashSet<>();

        udp = null;
    }

    @Override
    public void broadcast(ByteBuffer buffer) throws IllegalArgumentException {
        for(InetSocketAddress peer : eagerPeers)
            try {
                udp.send(buffer, peer);
            } catch(IOException | InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
    }

    @Override
    public int eagerPeerSetSize(int size) throws IllegalArgumentException {
        return 0;
    }

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
    public void initialize() {

    }

    @Override
    public InetSocketAddress id() {
        return id;
    }

    @Override
    public void notifyMessage(ByteBuffer bytes) {
        short type = bytes.getShort();

        if(type == BodyMessage.TYPE)
            try {
                BodyMessage msg = BodyMessage.parse(bytes);

                messages.put(msg);
            } catch(InterruptedException e) {
                // TODO
                e.printStackTrace();
            }
        else if(type == IHaveMessage.TYPE)
            ihaves.add(IHaveMessage.parse(bytes));
    }

    @Override
    public boolean setUDP(UDPInterface udp) throws IllegalArgumentException {
        this.udp = udp;
        return true;
    }
}
