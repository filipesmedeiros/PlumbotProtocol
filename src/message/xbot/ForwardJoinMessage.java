package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ForwardJoinMessage extends ControlMessage {

    public final static short TYPE = 3;

    private InetSocketAddress joiner;

    private int ttl;

    public ForwardJoinMessage(InetSocketAddress sender, InetSocketAddress joiner, int ttl) {
        super(sender, TYPE);

        this.joiner = joiner;
        this.ttl = ttl;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();
        putAddressInBuffer(buffer, joiner);

        buffer.getInt(ttl);

        buffer.put(EOT);

        buffer.flip();

        return buffer;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public ForwardJoinMessage next(InetSocketAddress sender) {
        return new ForwardJoinMessage(sender, joiner, ttl - 1);
    }

    public static ForwardJoinMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);
        InetSocketAddress joiner = parseAddress(bytes);

        int ttl = bytes.getInt();

        return new ForwardJoinMessage(sender, joiner, ttl);
    }
}
