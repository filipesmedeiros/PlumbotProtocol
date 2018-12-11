package message.plumtree;

import message.Message;
import message.xbot.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class IHaveMessage implements Message {

    public static final short TYPE = 101;
    // public static final int HASH_SIZE = 64; // 512 bit hash (in byte length) too hard, invest time later // TODO
    public static final int HASH_SIZE = 4; // 32 bit hash (in byte length) this is an integer

    private InetSocketAddress sender;

    private int hash;

    public IHaveMessage(InetSocketAddress sender, int hash) {
        this.sender = sender;
        this.hash = hash;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size());

        buffer.putShort(TYPE);

        char[] chars = sender().toString().toCharArray();
        for(char c : chars)
            buffer.putChar(c);

        buffer.putChar(EOS);

        buffer.putInt(hash);

        buffer.put(EOT);

        return buffer;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    public int hash() {
        return hash;
    }

    @Override
    public int size() {
        return Message.MSG_TYPE_SIZE + sender.toString().length() * 2 + 2
                + HASH_SIZE + 1;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public static IHaveMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = ControlMessage.parseAddress(bytes);

        int hash = bytes.getInt();

        return new IHaveMessage(sender, hash);
    }
}
