package messages.plumtree;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class BroadcastMessage extends MessageWithSender {

    private ByteBuffer data;
    private int round;

    public BroadcastMessage(InetSocketAddress sender, ByteBuffer data, int round) {
        super(sender);
        this.data = data;
        this.round = round;
    }

    public BroadcastMessage(InetSocketAddress sender, BroadcastMessage previousM) {
        super(sender);
        this.data = previousM.data;
        this.round = previousM.round + 1;
        this.id = previousM.id;
    }

    @Override
    public int size() {
        return baseSize() + data.position();
    }

    @Override
    public MessageType type() {
        return MessageType.Broadcast;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }

    public ByteBuffer data() {
        return data;
    }

    public int round() {
        return round;
    }
}
