package message.plumtree;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class GraftMessage extends PlumbotMessage {

    public static final short TYPE = 13;

    private int hash;

    private boolean send;

    public GraftMessage(InetSocketAddress sender, int hash, boolean send) {
        super(sender, TYPE);
        this.hash = hash;
        this.send = send;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.putInt(hash);

        buffer.put(send ? (byte) 1 : (byte) 0);

        buffer.put(EOT);

        return buffer;
    }

    public int hash() {
        return hash;
    }

    public boolean send() {
        return send;
    }

    @Override
    public int size() {
        return super.size() + IHaveMessage.HASH_SIZE;
    }

    public static GraftMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = TCP.parseAddress(bytes);

        int hash = bytes.getInt();

        boolean send = bytes.get() == 1;

        return new GraftMessage(sender, hash, send);
    }
}
