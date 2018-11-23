package message.xbot;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class OptimizationReplyMessage extends ControlMessage {

    public final static short TYPE = 8;

    private boolean accept;

    public OptimizationReplyMessage(InetSocketAddress sender, boolean accept) {
        super(sender, TYPE);
        this.accept = accept;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(accept ? (byte) 1 : (byte) 0);

        buffer.put(EOT).flip();

        return buffer;
    }

    public static OptimizationReplyMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        boolean accept = bytes.get() == 1;

        return new OptimizationReplyMessage(sender, accept);
    }
}
