package plumtree.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class BroadcastMessage extends ProtocolMessage {

    public static final short MSG_CODE = 103;

    private final UUID mId;
    private byte[] payload;
    private int round;

    public BroadcastMessage() {
        super(MSG_CODE);
        this.payload = new byte[0];
        this.round = 0;
        this.mId = UUID.randomUUID();
    }

    public BroadcastMessage(UUID mId, byte[] payload, int round) {
        super(MSG_CODE);
        this.mId = mId;
        this.payload = payload;
        this.round = round;
    }

    public byte[] payload() {
        return payload;
    }

    public BroadcastMessage setPayload(byte[] payload) {
        this.payload = new byte[payload.length];
        System.arraycopy(payload, 0, this.payload, 0, payload.length);
        return this;
    }

    public int round() {
        return round;
    }

    public BroadcastMessage setRound(int round) {
        this.round = round;
        return this;
    }

    public UUID mId() {
        return mId;
    }

    public static final ISerializer<BroadcastMessage> serializer = new ISerializer<BroadcastMessage>() {

        @Override
        public void serialize(BroadcastMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeInt(m.round);
            out.writeInt(m.payload.length);
            out.writeBytes(m.payload);
        }

        @Override
        public BroadcastMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            int round = in.readInt();
            int size = in.readInt();
            byte[] payload = new byte[size];
            in.readBytes(payload);
            return new BroadcastMessage(mId, payload, round);
        }

        @Override
        public int serializedSize(BroadcastMessage m) {
            return (2 * Long.BYTES) + (2 * Integer.BYTES) + m.payload.length;
        }
    };
}
