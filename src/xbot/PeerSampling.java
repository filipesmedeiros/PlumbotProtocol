package xbot;

import common.MessageProtocol;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface PeerSampling extends MessageProtocol {

    Collection<InetSocketAddress> getPeers();
}
