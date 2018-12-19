package message.plumtree;

import message.PlumbotMessage;
import network.TCP;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class RequestMessage extends PlumbotMessage {

    public static final short TYPE = 16;

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
        InetSocketAddress sender = TCP.parseAddress(bytes);

        int hash = bytes.getInt();

        return new RequestMessage(sender, hash);
    }
}
