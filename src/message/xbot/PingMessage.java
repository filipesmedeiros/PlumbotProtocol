package message.xbot;

import message.PlumbotMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PingMessage extends PlumbotMessage {

    public static final short TYPE = 4;

    public PingMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT).flip();

        return buffer;
    }

    public static PingMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        return new PingMessage(sender);
    }
}
