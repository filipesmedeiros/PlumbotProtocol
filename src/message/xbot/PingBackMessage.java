package message.xbot;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PingBackMessage extends ControlMessage {

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
        InetSocketAddress sender = parseAddress(bytes);

        return new PingBackMessage(sender);
    }
}
