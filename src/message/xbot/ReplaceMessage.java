package message.xbot;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ReplaceMessage extends PlumbotMessage {

    public static final short TYPE = 9;

    private InetSocketAddress init;
    private InetSocketAddress old;
    private long itoo;
    private long itoc;

    public ReplaceMessage(InetSocketAddress sender, InetSocketAddress init,
                          InetSocketAddress old, long itoo, long itoc) {
        super(sender, TYPE);
        this.init = init;
        this.old = old;
        this.itoo = itoo;
        this.itoc = itoc;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        putAddressInBuffer(buffer, init);

        putAddressInBuffer(buffer, old);

        buffer.putLong(itoo);
        buffer.putLong(itoc);

        buffer.put(EOT).flip();

        return buffer;
    }

    @Override
    public int size() {
        return super.size()
                + init.toString().length() * 2 + 2
                + old.toString().length() * 2 + 2
                + 8 + 8;
    }

    public InetSocketAddress init() {
        return init;
    }

    public InetSocketAddress old() {
        return old;
    }

    public long itoo() {
        return itoo;
    }

    public long itoc() {
        return itoc;
    }

    public static ReplaceMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        InetSocketAddress init = TCP.parseAddress(bytes);

        InetSocketAddress old = TCP.parseAddress(bytes);

        long itoo = bytes.getLong();
        long itoc = bytes.getLong();

        return new ReplaceMessage(sender, init, old, itoo, itoc);
    }
}
