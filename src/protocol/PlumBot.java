package protocol;

import interfaces.Node;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

public interface PlumBot extends Node {

    void broadcast(ByteBuffer bytes) throws IllegalArgumentException;

    void join(InetSocketAddress contact);

    Set<InetSocketAddress> peerActiveView();

    Set<InetSocketAddress> treeActiveView();

    Set<InetSocketAddress> passiveView();
}
