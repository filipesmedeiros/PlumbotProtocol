package message.plumtree;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class IHaveMessage extends HopMessage {

    public static final short TYPE = 101;
    // public static final int HASH_SIZE = 64; // 512 bit hash (in byte length) too hard, invest time later // TODO
    static final int HASH_SIZE = 4; // 32 bit hash (in byte length) this is an integer

    private InetSocketAddress original;

    private int hash;

    public IHaveMessage(InetSocketAddress sender, int hash, short hops, InetSocketAddress original) {
        super(sender, TYPE, hops);
        this.original = original;
        this.hash = hash;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        putAddressInBuffer(buffer, original);

        buffer.putInt(hash);

        buffer.putShort(hops());

        buffer.put(EOT);

        return buffer;
    }

    public int hash() {
        return hash;
    }

    @Override
    public int size() {
        return super.size() + original.toString().length() * 2 + 2
                + HASH_SIZE + 2;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public IHaveMessage next(InetSocketAddress sender) {
        nextHop();
        this.sender = sender;
        return this;
    }

    public static IHaveMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = parseAddress(bytes);

        InetSocketAddress original = parseAddress(bytes);

        int hash = bytes.getInt();

        short hops = bytes.getShort();

        return new IHaveMessage(sender, hash, hops, original);
    }
}
