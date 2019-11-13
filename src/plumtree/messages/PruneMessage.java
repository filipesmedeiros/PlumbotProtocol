package plumtree.messages;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PruneMessage extends MessageWithSender {

    public PruneMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public int size() {
        return baseSize();
    }

    @Override
    public MessageType type() {
        return MessageType.Prune;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }
}
