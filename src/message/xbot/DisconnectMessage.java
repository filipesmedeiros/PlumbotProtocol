package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DisconnectMessage extends ControlMessage {

    public static final short TYPE = 7;

    public DisconnectMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT);

        buffer.flip();

        return buffer;
    }

    public static DisconnectMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        return new DisconnectMessage(sender);
    }
}
