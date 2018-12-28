package message.xbot;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ReplaceReplyMessage extends PlumbotMessage {

    public final static short TYPE = 10;

    private boolean accept;

    public ReplaceReplyMessage(InetSocketAddress sender, boolean accept) {
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

    public boolean accept() {
        return accept;
    }

    @Override
    public int size() {
        return super.size() + 1;
    }

    public static ReplaceReplyMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        short cycle = bytes.getShort();

        boolean accept = bytes.get() == 1;

        return new ReplaceReplyMessage(sender, accept);
    }
}
