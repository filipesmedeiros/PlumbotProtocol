package message.plumtree;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class RequestMessage extends ControlMessage {

    public static final short TYPE = 103;

    private int hash;

    public RequestMessage(InetSocketAddress sender, int hash) {
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

    public static RequestMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        int hash = bytes.getInt();

        return new RequestMessage(sender, hash);
    }
}
