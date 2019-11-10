package messages.plumtree;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class GraftMessage extends MessageWithSender {

    private UUID graftId;

    public GraftMessage(InetSocketAddress sender, UUID graftId) {
        super(sender);
        this.graftId = graftId;
    }

    @Override
    public int size() {
        return baseSize() + 16;
    }

    @Override
    public MessageType type() {
        return MessageType.Graft;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }

    public UUID graftId() {
        return graftId;
    }
}
