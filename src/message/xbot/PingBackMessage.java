package message.xbot;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PingBackMessage extends PlumbotMessage {

    public static final short TYPE = 5;

    public PingBackMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT).flip();

        return buffer;
    }

    public static PingBackMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        return new PingBackMessage(sender);
    }
}
