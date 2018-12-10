package message.plumtree;

import message.Message;
import message.xbot.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class IHaveMessage implements Message {

    public static final short TYPE = 101;
    public static final int HASH_SIZE = 64; // 512 bit hash (in byte length)

    private InetSocketAddress sender;

    private String hash;

    public IHaveMessage(InetSocketAddress sender, String hash)
            throws IllegalArgumentException {
        if(hash.length() > HASH_SIZE / 2)
            throw new IllegalArgumentException();

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

        chars = hash.toCharArray();
        for(char c : chars)
            buffer.putChar(c);

        buffer.putChar(EOS);

        buffer.put(EOT);

        return buffer;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    public String hash() {
        return hash;
    }

    @Override
    public int size() {
        return Message.MSG_TYPE_SIZE + sender.toString().length() * 2 + 2
                + HASH_SIZE + 2 + 1;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public static IHaveMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = ControlMessage.parseAddress(bytes);

        byte[] body = new byte[bytes.getInt()];


        return new IHaveMessage(sender, ByteBuffer.wrap(body), body.length);
    }
}
