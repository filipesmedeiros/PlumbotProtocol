package xbot.messages;

import babel.protocol.event.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;
import java.util.UUID;

public class DisconnectMessage extends ProtocolMessage {

    public static final short MSG_CODE = 201;

    private UUID mId;
    private boolean wait;

    public DisconnectMessage() {
        super(MSG_CODE);
        this.wait = false;
        this.mId = UUID.randomUUID();
    }

    public DisconnectMessage(UUID mId, boolean wait) {
        super(MSG_CODE);
        this.mId = mId;
        this.wait = wait;
    }

    public boolean isWait() {
        return wait;
    }

    public DisconnectMessage setWait(boolean wait) {
        this.wait = wait;
        return this;
    }

    public UUID mId() {
        return mId;
    }

    public static final ISerializer<DisconnectMessage> serializer = new ISerializer<DisconnectMessage>() {

        @Override
        public void serialize(DisconnectMessage m, ByteBuf out) {
            out.writeLong(m.mId.getMostSignificantBits());
            out.writeLong(m.mId.getLeastSignificantBits());
            out.writeBoolean(m.wait);
        }

        @Override
        public DisconnectMessage deserialize(ByteBuf in) throws UnknownHostException {
            UUID mId = new UUID(in.readLong(), in.readLong());
            boolean wait = in.readBoolean();
            return new DisconnectMessage(mId, wait);
        }

        @Override
        public int serializedSize(DisconnectMessage m) {
            return (2 * Long.BYTES) + 1;
        }
    };
}
