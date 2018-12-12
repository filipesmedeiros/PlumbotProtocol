package message.xbot;

import message.PlumbotMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ForwardJoinMessage extends PlumbotMessage {

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

        buffer.putInt(ttl);

        buffer.put(EOT).flip();

        return buffer;
    }

    @Override
    public int size() {
        return super.size() + joiner.toString().length() * 2 + 2 + 4;
    }

    public ForwardJoinMessage next(InetSocketAddress sender) {
        return new ForwardJoinMessage(sender, joiner, ttl - 1);
    }

    public int ttl() {
        return ttl;
    }

    public InetSocketAddress joiner() {
        return joiner;
    }

    public static ForwardJoinMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);
        InetSocketAddress joiner = parseAddress(bytes);

        int ttl = bytes.getInt();

        return new ForwardJoinMessage(sender, joiner, ttl);
    }
}
