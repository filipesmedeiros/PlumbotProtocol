package message.plumtree;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class GraftMessage extends ControlMessage {

    public static final short TYPE = 104;

    private int hash;

    public GraftMessage(InetSocketAddress sender, int hash) {
        super(sender, TYPE);
        this.hash = hash;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.putInt(hash);

        buffer.put(EOT);

        return buffer;
    }

    public int hash() {
        return hash;
    }

    @Override
    public int size() {
        return super.size() + IHaveMessage.HASH_SIZE;
    }

    public static GraftMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        int hash = bytes.getInt();

        return new GraftMessage(sender, hash);
    }
}
