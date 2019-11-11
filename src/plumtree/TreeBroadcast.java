package plumtree;

import common.MessageProtocol;
import messages.Message;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface TreeBroadcast extends MessageProtocol {

    void broadcast(ByteBuffer data);

    void peerDown(InetSocketAddress peer);

    void peerUp(InetSocketAddress peer);
}
