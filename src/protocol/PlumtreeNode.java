package protocol;

import message.Message;
import message.plumtree.BodyMessage;
import network.UDP;
import network.UDPInterface;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class PlumtreeNode implements TreeBroadcastNode {

    private InetSocketAddress id;

    private Set<InetSocketAddress> eagerPeers;

    private BlockingQueue<BodyMessage> messages;

    private UDPInterface udp;

    public PlumtreeNode(InetSocketAddress id, int eagerPeerSetSize) {
        this.id = id;

        eagerPeers = new HashSet<>(eagerPeerSetSize);

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
    public void notifyMessage(ByteBuffer msg) {

    }

    @Override
    public boolean setUDP(UDPInterface udp) throws IllegalArgumentException {
        return false;
    }
}
