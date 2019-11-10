package messages.plumtree;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class IHaveMessage extends MessageWithSender {

    private UUID iHaveId;
    private int round;

    public IHaveMessage(InetSocketAddress sender, UUID iHaveId, int round) {
        super(sender);
        this.iHaveId = iHaveId;
        this.round = round;
    }

    @Override
    public int size() {
        return baseSize() + 16;
    }

    @Override
    public MessageType type() {
        return null;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }

    public UUID iHaveId() {
        return iHaveId;
    }

    public int round() {
        return round;
    }
}
