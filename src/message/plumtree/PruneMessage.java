package message.plumtree;

import message.Message;
import message.xbot.ControlMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PruneMessage implements Message {

    public static final short TYPE = 102;

    private InetSocketAddress sender;

    public PruneMessage(InetSocketAddress sender)
            throws IllegalArgumentException {
        this.sender = sender;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(size());

        buffer.putShort(TYPE);

        char[] chars = sender().toString().toCharArray();
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

    @Override
    public int size() {
        return Message.MSG_TYPE_SIZE + sender.toString().length() * 2 + 2 + 1;
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public static PruneMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = ControlMessage.parseAddress(bytes);

        return new PruneMessage(sender);
    }
}
