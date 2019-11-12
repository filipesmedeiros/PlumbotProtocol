package messages.xbot.oracle;

import messages.MessageWithSender;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PingMessage extends MessageWithSender {

    public PingMessage(InetSocketAddress sender) {
        super(sender);
    }

    @Override
    public int size() {
        return baseSize();
    }

    @Override
    public MessageType type() {
        return MessageType.Ping;
    }

    @Override
    public ByteBuffer serialize() {
        // TODO

        return null;
    }
}
