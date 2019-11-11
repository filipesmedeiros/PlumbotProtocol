package xbot;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface PeerSampling {

    Collection<InetSocketAddress> getPeers();
}
