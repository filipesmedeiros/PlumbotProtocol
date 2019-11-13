package plumtree.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class IHaveMessage extends ProtocolMessage {

    public static final short MSG_CODE = 102;

    private final UUID mId;
    private UUID iHaveId;
    private int round;

    public IHaveMessage() {
        super(MSG_CODE);
        this.mId = UUID.randomUUID();
        this.iHaveId = null;
        this.round = 0;
    }

    public IHaveMessage(UUID mId, UUID iHaveId, int round) {
        super(MSG_CODE);
        this.mId = UUID.randomUUID();
        this.iHaveId = iHaveId;
        this.round = round;
    }

    public IHaveMessage(BroadcastMessage broadcastMessage) {
        super(MSG_CODE);
        this.iHaveId = broadcastMessage.mId();
        this.round = broadcastMessage.round() + 1;
        this.mId = UUID.randomUUID();
    }

    public UUID iHaveId() {
        return iHaveId;
    }

    public IHaveMessage setIHaveId(UUID iHaveId) {
        this.iHaveId = iHaveId;
        return this;
    }

    public int round() {
        return round;
    }

    public IHaveMessage setRound(int round) {
        this.round = round;
        return this;
    }

    public static final ISerializer<IHaveMessage> serializer = new ISerializer<IHaveMessage>() {

        @Override
        public void serialize(IHaveMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeLong(m.iHaveId.getMostSignificantBits());
            out.writeLong(m.iHaveId.getLeastSignificantBits());
            out.writeInt(m.round);
        }

        @Override
        public IHaveMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            UUID iHaveId = new UUID(in.readLong(), in.readLong());
            int round = in.readInt();
            return new IHaveMessage(mId, iHaveId, round);
        }

        @Override
        public int serializedSize(IHaveMessage m) {
            return (4 * Long.BYTES) + Integer.BYTES;
        }
    };
}
