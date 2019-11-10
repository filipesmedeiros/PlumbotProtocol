package xbot;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface PeerSamplingNode {

    Collection<InetSocketAddress> getPeers();
}
