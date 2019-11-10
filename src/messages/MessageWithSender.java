package messages;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class MessageWithSender implements Message {

    public static final int BYTE_SIZE = 14;

    private InetSocketAddress sender;
    protected UUID id;

    public MessageWithSender(InetSocketAddress sender) {
        this.sender = sender;
        id = UUID.randomUUID();
    }

    @Override
    public InetSocketAddress sender() {
        return sender;
    }

    @Override
    public UUID id() {
        return id;
    }

    public ByteBuffer serializeTypeIdAndSender(ByteBuffer buffer) {
        return buffer.putShort(type().code())
                .put(sender.getAddress().getAddress())
                .putShort((short) sender.getPort())
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits());
    }

    public int baseSize() {
        return 24; // 2 type + 6 sender + 16 id
    }

    abstract public int size();
}
