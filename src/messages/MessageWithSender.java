package messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class MessageWithSender implements Message {

    public static final int BYTE_SIZE = 14;

    private InetSocketAddress sender;
    private long id;

    public MessageWithSender(InetSocketAddress sender) {
        this.sender = sender;
        id = UUID.randomUUID().getMostSignificantBits();
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public ByteBuffer serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(size());
        buffer.putShort(type().code());
        return serializeIdAndSender(buffer);
    }

    public ByteBuffer serializeIdAndSender(ByteBuffer buffer) {
        return buffer.put(sender.getAddress().getAddress())
                .putShort((short) sender.getPort())
                .putLong(id);
    }

    abstract public int size();}
