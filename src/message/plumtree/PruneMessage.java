package message.plumtree;

import message.PlumbotMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PruneMessage extends PlumbotMessage {

    public static final short TYPE = 15;

    public PruneMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT).flip();

        return buffer;
    }

    public static PruneMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        return new PruneMessage(sender);
    }
}
