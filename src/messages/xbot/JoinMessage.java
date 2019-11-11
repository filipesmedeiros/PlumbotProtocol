package messages.xbot;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class JoinMessage extends MessageWithSender {

    public JoinMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public MessageType type() {
        return MessageType.Join;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }

    @Override
    public int size() {
        return baseSize();
    }
}
