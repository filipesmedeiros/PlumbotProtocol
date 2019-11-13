package xbot.oracle.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class PingReplyMessage extends ProtocolMessage {

    public static final short MSG_CODE = 302;

    private long cost;
    private UUID mId;

    public PingReplyMessage() {
        super(MSG_CODE);
        this.cost = 0;
        this.mId = UUID.randomUUID();
    }

    public PingReplyMessage(UUID mId, long cost) {
        super(MSG_CODE);
        this.mId = mId;
        this.cost = cost;
    }

    public UUID mId() {
        return mId;
    }

    public long cost() {
        return cost;
    }

    public PingReplyMessage setCost(long cost) {
        this.cost = cost;
        return this;
    }

    public static final ISerializer<PingReplyMessage> serializer = new ISerializer<PingReplyMessage>() {

        @Override
        public void serialize(PingReplyMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeLong(m.cost);
        }

        @Override
        public PingReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            long cost = in.readLong();
            return new PingReplyMessage(mId, cost);
        }

        @Override
        public int serializedSize(PingReplyMessage m) {
            return (2 * Long.BYTES);
        }
    };
}
