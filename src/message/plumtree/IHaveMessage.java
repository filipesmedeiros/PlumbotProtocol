package message.plumtree;

import message.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class IHaveMessage extends ControlMessage {

    public static final short TYPE = 101;
    // public static final int HASH_SIZE = 64; // 512 bit hash (in byte length) too hard, invest time later // TODO
    static final int HASH_SIZE = 4; // 32 bit hash (in byte length) this is an integer

    private InetSocketAddress sender;

    private int hash;

    public IHaveMessage(InetSocketAddress sender, int hash) {
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
        return super.size() + HASH_SIZE;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public static IHaveMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        int hash = bytes.getInt();

        return new IHaveMessage(sender, hash);
    }
}
