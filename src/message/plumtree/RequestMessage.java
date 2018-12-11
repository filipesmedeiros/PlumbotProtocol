package message.plumtree;

import message.Message;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class RequestMessage implements Message {

    public static final short TYPE = 103;

    public InetSocketAddress sender;

    private int hash;

    public RequestMessage(InetSocketAddress sender, int hash) {
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
                + IHaveMessage.HASH_SIZE + 1;
    }

    @Override
    public short messageType() {
        return TYPE;
    }
}
