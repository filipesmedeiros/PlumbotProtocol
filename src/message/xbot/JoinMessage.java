package message.xbot;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class JoinMessage extends ControlMessage {

    public final static short TYPE = 1;

    public JoinMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT).flip();

        return buffer;
    }

    public static JoinMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        return new JoinMessage(sender);
    }


}
