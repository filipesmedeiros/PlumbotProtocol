package messages.xbot;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// TODO
public class ForwardJoinMessage extends MessageWithSender {

    private int ttl;
    private InetSocketAddress joiningPeer;

    public ForwardJoinMessage(InetSocketAddress sender, int ttl, InetSocketAddress joiningPeer) {
        super(sender);
        this.ttl = ttl;
        this.joiningPeer = joiningPeer;
    }

    public ForwardJoinMessage(InetSocketAddress sender, ForwardJoinMessage previousM) {
        super(sender);
        this.ttl = previousM.ttl - 1;
        joiningPeer = previousM.joiningPeer;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public MessageType type() {
        return null;
    }

    @Override
    public ByteBuffer serialize() {
        return null;
    }

    public int ttl() {
        return ttl;
    }

    public InetSocketAddress joiningPeer() {
        return joiningPeer;
    }
}
