package message.xbot;

import message.PlumbotMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SwitchMessage extends PlumbotMessage {

    public static final short TYPE = 11;

    private InetSocketAddress init;
    private long dtoo;

    public SwitchMessage(InetSocketAddress sender, InetSocketAddress init, long dtoo) {
        super(sender, TYPE);
        this.init = init;
        this.dtoo = dtoo;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();
        putAddressInBuffer(buffer, init);

        buffer.putLong(dtoo);

        buffer.put(EOT).flip();

        return buffer;
    }

    @Override
    public int size() {
        return super.size() + init.toString().length() * 2 + 2
                + 8;
    }

    public InetSocketAddress init() {
        return init;
    }

    public long dtoo() {
        return dtoo;
    }

    public static SwitchMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);
        InetSocketAddress init = parseAddress(bytes);

        long dtoo = bytes.getLong();

        return new SwitchMessage(sender, init, dtoo);
    }
}
