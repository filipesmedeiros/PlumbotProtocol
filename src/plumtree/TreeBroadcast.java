package plumtree;

import messages.Message;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface TreeBroadcast {

    void broadcast(ByteBuffer data);

    void peerDown(InetSocketAddress peer);

    void peerUp(InetSocketAddress peer);

    void deliverMessage(Message message);

    boolean breaksTree(InetSocketAddress peer);
}
