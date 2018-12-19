package message.xbot;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class AcceptJoinMessage extends PlumbotMessage {

    public final static short TYPE = 2;

    public AcceptJoinMessage(InetSocketAddress sender) {
        super(sender, TYPE);
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(EOT).flip();

        return buffer;
    }

    public static AcceptJoinMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        return new AcceptJoinMessage(sender);
    }
}
