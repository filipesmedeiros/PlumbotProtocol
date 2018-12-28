package message.xbot;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class DisconnectMessage extends PlumbotMessage {

    public static final short TYPE = 7;

    private boolean wait;

    public DisconnectMessage(InetSocketAddress sender, boolean wait) {
        super(sender, TYPE);

        this.wait = wait;
    }

    @Override
    public int size() {
        return super.size() + 1;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.put(wait ? (byte) 1 : (byte) 0);

        buffer.put(EOT).flip();

        return buffer;
    }

    public boolean hasToWait() {
        return wait;
    }

    public static DisconnectMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        short cycle = bytes.getShort();

        boolean wait = bytes.get() == 1;

        return new DisconnectMessage(sender, wait);
    }
}
