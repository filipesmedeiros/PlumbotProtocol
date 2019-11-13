package plumtree.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class GraftMessage extends ProtocolMessage {

    public static final short MSG_CODE = 101;

    private final UUID mId;
    private UUID graftId;
    private int round;

    public GraftMessage() {
        super(MSG_CODE);
        this.graftId = null;
        this.round = 0;
        this.mId = UUID.randomUUID();
    }

    public GraftMessage(UUID mId, UUID graftId, int round) {
        super(MSG_CODE);
        this.mId = mId;
        this.graftId = graftId;
        this.round = round;
    }

    public UUID graftId() {
        return graftId;
    }

    public GraftMessage setGraftId(UUID graftId) {
        this.graftId = graftId;
        return this;
    }

    public UUID mId() {
        return mId;
    }

    public int round() {
        return round;
    }

    public GraftMessage setRound(int round) {
        this.round = round;
        return this;
    }

    public static final ISerializer<GraftMessage> serializer = new ISerializer<GraftMessage>() {

        @Override
        public void serialize(GraftMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeLong(m.graftId.getMostSignificantBits());
            out.writeLong(m.graftId.getLeastSignificantBits());
            out.writeInt(m.round);
        }

        @Override
        public GraftMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            UUID graftId = new UUID(in.readLong(), in.readLong());
            int round = in.readInt();
            return new GraftMessage(mId, graftId, round);
        }

        @Override
        public int serializedSize(GraftMessage m) {
            return (4 * Long.BYTES) + Integer.BYTES;
        }
    };
}
