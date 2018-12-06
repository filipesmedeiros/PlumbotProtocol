package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class AcceptJoinMessage extends ControlMessage {

    public final static short TYPE = 2;

    private long cost;

    public AcceptJoinMessage(InetSocketAddress sender, long cost) {
        super(sender, TYPE);
        this.cost = cost;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.putLong(cost);

        buffer.put(EOT).flip();

        return buffer;
    }

    public long cost() {
        return cost;
    }

    public static AcceptJoinMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        long cost = bytes.getLong();

        return new AcceptJoinMessage(sender, cost);
    }
}
