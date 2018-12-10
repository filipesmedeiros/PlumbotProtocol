package message.xbot;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class OptimizationReplyMessage extends ControlMessage {

    public final static short TYPE = 8;

    private boolean accept;
    private boolean removed;

    public OptimizationReplyMessage(InetSocketAddress sender, boolean accept, boolean removed) {
        super(sender, TYPE);
        this.accept = accept;
        this.removed = removed;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(accept ? (byte) 1 : (byte) 0);

        buffer.put(removed ? (byte) 1 : (byte) 0);

        buffer.put(EOT).flip();

        return buffer;
    }

    @Override
    public int size() {
        return super.size() + 1 + 1;
    }

    public boolean accept() {
        return accept;
    }

    public boolean removed() {
        return removed;
    }

    public static OptimizationReplyMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        boolean accept = bytes.get() == 1;

        boolean removed = bytes.get() == 1;

        return new OptimizationReplyMessage(sender, accept, removed);
    }
}
