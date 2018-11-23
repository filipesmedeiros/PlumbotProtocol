package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class OptimizationMessage extends ControlMessage {

    public static final short TYPE = 6;

    private InetSocketAddress old;
    private long itoo;
    private long itoc;

    public OptimizationMessage(InetSocketAddress sender, InetSocketAddress old, long itoo, long itoc) {
        super(sender, TYPE);
        this.old = old;
        this.itoo = itoo;
        this.itoc = itoc;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();
        putAddressInBuffer(buffer, old);

        buffer.putLong(itoo);
        buffer.putLong(itoc);

        buffer.put(EOT);

        buffer.flip();

        return buffer;
    }

    @Override
    public int size() {
        return 4 + sender().toString().length() * 2 + 2 + old.toString().length() * 2 + 8 + 8 + 1;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public static OptimizationMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        InetSocketAddress old = parseAddress(bytes);

        long itoo = bytes.getLong();
        long itoc = bytes.getLong();

        return new OptimizationMessage(sender, old, itoo, itoc);
    }
}
