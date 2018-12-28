package interfaces;

import java.net.InetSocketAddress;

public interface NeighbourhoodListener extends Node {

    void neighbourUp(InetSocketAddress peer);

    void neighbourDown(InetSocketAddress peer);

    boolean canOptimize(InetSocketAddress peer1, InetSocketAddress peer2);
}
