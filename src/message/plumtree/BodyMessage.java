package message.plumtree;

import message.Message;
import message.PlumbotMessage;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class BodyMessage extends HopMessage {

    public static final short TYPE = 100;

    private ByteBuffer body;
    private int bodySize;

    public BodyMessage(InetSocketAddress sender, ByteBuffer body, int bodySize, short hops)
            throws IllegalArgumentException {
        super(sender, TYPE, hops);
        if(calcSize(sender, body) >= Message.MSG_SIZE)
            throw new IllegalArgumentException();

        this.body = body;
        this.bodySize = bodySize;
    }

    @Override
    public ByteBuffer bytes() {
        ByteBuffer buffer = putSenderInBuffer();

        buffer.putInt(bodySize);

        buffer.putShort(hops());

        buffer.put(body);

        buffer.put(Message.EOT);

        return buffer;
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    public int bodySize() {
        return bodySize;
    }

    @Override
    public int size() {
        return calcSize(sender, body);
    }

    @Override
    public short messageType() {
        return TYPE;
    }

    public BodyMessage next(InetSocketAddress sender) {
        nextHop();
        this.sender = sender;
        return this;
    }

    public static BodyMessage parse(ByteBuffer bytes) {
        InetSocketAddress sender = PlumbotMessage.parseAddress(bytes);

        byte[] body = new byte[bytes.getInt()];

        short hops = bytes.getShort();

        bytes.get(body);

        return new BodyMessage(sender, ByteBuffer.wrap(body), body.length, hops);
    }

    private static int calcSize(InetSocketAddress sender, ByteBuffer body) {
        return Message.MSG_TYPE_SIZE + sender.toString().length() * 2 + 2 + 4 + 2 + body.limit() + 1;
    }
}
